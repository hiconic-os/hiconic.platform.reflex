package hiconic.rx.log.reflection.processing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.LoggerFactory;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.LifecycleAware;
import com.braintribe.cfg.Required;
import com.braintribe.model.logging.LogLevel;
import com.braintribe.model.service.api.InstanceId;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogPosition;
import hiconic.rx.log.reflection.model.LogRecord;
import hiconic.rx.log.reflection.model.LogSourceLocation;
import hiconic.rx.log.reflection.model.api.LogRecordPage;

/**
 * Captures Logback events before an encoder removes structural information. Collection is deliberately lease based: the
 * appender is attached by the first live query and detached after a quiet period. Consequently log reflection has no
 * permanent record-allocation or ring-buffer cost when nobody observes the live stream.
 * <p>
 * Every activation creates a new cursor generation. A cursor from an expired generation, or one that fell behind the
 * bounded ring buffer, produces an explicit continuity gap. Consumers must never interpret records returned together
 * with such a gap as a complete continuation; durable history belongs to the file streams.
 */
public class StructuredLiveLogCollector implements LifecycleAware {
	public static final String STREAM_ID = "structured-live";
	private static final String CURSOR_PREFIX = "live-v1:";

	private final Object monitor = new Object();
	private final Object lifecycleMonitor = new Object();
	private final ArrayDeque<LogRecord> records = new ArrayDeque<>();
	private final AtomicLong sequence = new AtomicLong();
	private final List<Logger> attachedLoggers = new ArrayList<>();

	private InstanceId instanceId;
	private LoggerContext loggerContext;
	private int capacity = 10_000;
	private long leaseMillis = 30_000;
	private AppenderBase<ILoggingEvent> appender;
	private ScheduledThreadPoolExecutor leaseScheduler;
	private ScheduledFuture<?> leaseExpiry;
	private long leaseDeadlineNanos;
	private String generation;
	private boolean active;
	private boolean destroyed;

	@Configurable
	@Required
	public void setInstanceId(InstanceId instanceId) {
		this.instanceId = instanceId;
	}

	@Configurable
	public void setLoggerContext(LoggerContext loggerContext) {
		this.loggerContext = loggerContext;
	}

	@Configurable
	public void setCapacity(int capacity) {
		if (capacity < 1)
			throw new IllegalArgumentException("Structured live log capacity must be positive");
		this.capacity = capacity;
	}

	@Configurable
	public void setLeaseMillis(long leaseMillis) {
		if (leaseMillis < 1)
			throw new IllegalArgumentException("Structured live log lease must be positive");
		this.leaseMillis = leaseMillis;
	}

	@Override
	public void postConstruct() {
		if (loggerContext == null)
			loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		ThreadFactory threadFactory = runnable -> {
			Thread thread = new Thread(runnable, "log-reflection-live-lease");
			thread.setDaemon(true);
			return thread;
		};
		leaseScheduler = new ScheduledThreadPoolExecutor(1, threadFactory);
		leaseScheduler.setRemoveOnCancelPolicy(true);
		leaseScheduler.setKeepAliveTime(1, TimeUnit.SECONDS);
		leaseScheduler.allowCoreThreadTimeOut(true);
	}

	@Override
	public void preDestroy() {
		synchronized (lifecycleMonitor) {
			destroyed = true;
			if (leaseExpiry != null)
				leaseExpiry.cancel(false);
			leaseExpiry = null;
			deactivateLocked();
		}
		if (leaseScheduler != null)
			leaseScheduler.shutdownNow();
	}

	private void attach(Logger logger) {
		logger.addAppender(appender);
		attachedLoggers.add(logger);
	}

	private void capture(ILoggingEvent event, String collectorGeneration) {
		event.prepareForDeferredProcessing();

		LogRecord record = LogRecord.T.create();
		record.setTimestamp(new Date(event.getTimeStamp()));
		record.setLevel(toModelLevel(event));
		record.setOrigin(LogReflectionModelTools.origin(instanceId));
		record.setStreamId(STREAM_ID);
		record.setLoggerName(event.getLoggerName());
		record.setThreadName(event.getThreadName());
		record.setMessage(event.getFormattedMessage());
		record.setProperties(new LinkedHashMap<>(event.getMDCPropertyMap()));

		IThrowableProxy throwableProxy = event.getThrowableProxy();
		if (throwableProxy != null)
			record.setThrowable(ThrowableProxyUtil.asString(throwableProxy));

		if (event.hasCallerData()) {
			StackTraceElement[] callerData = event.getCallerData();
			if (callerData.length > 0)
				record.setSourceLocation(sourceLocation(callerData[0]));
		}

		synchronized (monitor) {
			if (!active || !collectorGeneration.equals(generation))
				return;
			LogPosition position = LogPosition.T.create();
			position.setSequence(sequence.incrementAndGet());
			record.setPosition(position);
			while (records.size() >= capacity)
				records.removeFirst();
			records.addLast(record);
			monitor.notifyAll();
		}
	}

	private static LogSourceLocation sourceLocation(StackTraceElement caller) {
		LogSourceLocation source = LogSourceLocation.T.create();
		source.setClassName(caller.getClassName());
		source.setMethodName(caller.getMethodName());
		source.setFileName(caller.getFileName());
		source.setLineNumber(caller.getLineNumber());
		return source;
	}

	private static LogLevel toModelLevel(ILoggingEvent event) {
		return LogLevel.valueOf(event.getLevel().levelStr);
	}

	public LogRecordPage query(LogFilter filter, String cursor, int requestedLimit, long waitMillis) {
		return query(filter, cursor, null, requestedLimit, waitMillis);
	}

	public LogRecordPage query(LogFilter filter, String cursor, String beforeCursor, int requestedLimit, long waitMillis) {
		renewLease();
		int limit = Math.max(1, Math.min(requestedLimit, 10_000));
		LiveCursor afterCursor = parseCursor(cursor);
		LiveCursor before = parseCursor(beforeCursor);
		boolean continuityLost = false;

		MatchWindow window;
		String currentGeneration;
		synchronized (monitor) {
			currentGeneration = generation;
			Long afterSequence = compatibleSequence(afterCursor, currentGeneration);
			Long beforeSequence = compatibleSequence(before, currentGeneration);
			continuityLost = incompatible(afterCursor, currentGeneration) || incompatible(before, currentGeneration);

			if (afterSequence != null && !records.isEmpty()) {
				long oldest = records.getFirst().getPosition().getSequence();
				long newest = records.getLast().getPosition().getSequence();
				if (afterSequence < oldest - 1) {
					continuityLost = true;
					afterSequence = oldest - 1;
				} else if (afterSequence > newest) {
					continuityLost = true;
					afterSequence = newest;
				}
			}
			window = matches(filter, afterSequence, beforeSequence);
			if (window.matches.isEmpty() && afterSequence != null && beforeSequence == null && waitMillis > 0) {
				try {
					monitor.wait(Math.min(waitMillis, 30_000));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				window = matches(filter, afterSequence, null);
			}
			List<LogRecord> matches = window.matches;

			boolean backwards = beforeSequence != null;
			boolean initial = afterSequence == null && !backwards;
			boolean moreAvailable = afterSequence != null && matches.size() > limit;
			boolean moreBeforeAvailable = (initial || backwards) && matches.size() > limit;
			List<LogRecord> page;
			if ((initial || backwards) && matches.size() > limit)
				page = new ArrayList<>(matches.subList(matches.size() - limit, matches.size()));
			else
				page = new ArrayList<>(matches.subList(0, Math.min(limit, matches.size())));

			LogRecordPage response = LogRecordPage.T.create();
			response.setRecords(page);
			response.setMoreAvailable(moreAvailable);
			response.setMoreBeforeAvailable(moreBeforeAvailable);
			response.setObservedProperties(LogRecordFiltering.observedProperties(page));
			if (!backwards && moreAvailable)
				response.setNextCursor(formatCursor(currentGeneration, page.get(page.size() - 1).getPosition().getSequence()));
			else if (!backwards && window.highWatermark != null)
				response.setNextCursor(formatCursor(currentGeneration, window.highWatermark));
			else if (!backwards)
				response.setNextCursor(formatCursor(currentGeneration, sequence.get()));
			else
				response.setNextCursor(cursor);
			if (!page.isEmpty() && moreBeforeAvailable)
				response.setPreviousCursor(formatCursor(currentGeneration, page.get(0).getPosition().getSequence()));
			if (continuityLost)
				response.setContinuityGaps(Map.of(instanceId.stringify(),
						"The structured live-log cursor is no longer continuous; collection restarted or older records were evicted."));
			return response;
		}
	}

	private void renewLease() {
		synchronized (lifecycleMonitor) {
			if (destroyed)
				throw new IllegalStateException("Structured live log collector is already destroyed");
			if (!active)
				activateLocked();
			leaseDeadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(leaseMillis);
			if (leaseExpiry == null)
				leaseExpiry = leaseScheduler.schedule(this::expireLease, leaseMillis, TimeUnit.MILLISECONDS);
		}
	}

	private void activateLocked() {
		String collectorGeneration = UUID.randomUUID().toString();
		synchronized (monitor) {
			records.clear();
			sequence.set(0);
			generation = collectorGeneration;
			active = true;
		}

		appender = new AppenderBase<>() {
			@Override
			protected void append(ILoggingEvent event) {
				capture(event, collectorGeneration);
			}
		};
		appender.setContext(loggerContext);
		appender.setName("hiconic-log-reflection");
		appender.start();

		attach(loggerContext.getLogger(Logger.ROOT_LOGGER_NAME));
		for (Logger logger : loggerContext.getLoggerList()) {
			if (!logger.isAdditive() && !Logger.ROOT_LOGGER_NAME.equals(logger.getName()))
				attach(logger);
		}
	}

	private void expireLease() {
		synchronized (lifecycleMonitor) {
			leaseExpiry = null;
			if (destroyed || !active)
				return;
			long remaining = leaseDeadlineNanos - System.nanoTime();
			if (remaining > 0) {
				leaseExpiry = leaseScheduler.schedule(this::expireLease, remaining, TimeUnit.NANOSECONDS);
				return;
			}
			deactivateLocked();
		}
	}

	private void deactivateLocked() {
		for (Logger logger : attachedLoggers)
			logger.detachAppender(appender);
		attachedLoggers.clear();
		if (appender != null)
			appender.stop();
		appender = null;
		synchronized (monitor) {
			active = false;
			records.clear();
		}
	}

	private MatchWindow matches(LogFilter filter, Long afterSequence, Long beforeSequence) {
		List<LogRecord> result = new ArrayList<>();
		Long highWatermark = afterSequence;
		for (LogRecord record : records) {
			long sequence = record.getPosition().getSequence();
			if (afterSequence != null && sequence <= afterSequence)
				continue;
			if (beforeSequence != null && sequence >= beforeSequence)
				continue;
			highWatermark = sequence;
			if (LogRecordFiltering.matches(record, filter))
				result.add(record);
		}
		return new MatchWindow(result, highWatermark);
	}

	private record MatchWindow(List<LogRecord> matches, Long highWatermark) {
	}

	private static Long compatibleSequence(LiveCursor cursor, String generation) {
		return cursor != null && generation.equals(cursor.generation) ? cursor.sequence : null;
	}

	private static boolean incompatible(LiveCursor cursor, String generation) {
		return cursor != null && !generation.equals(cursor.generation);
	}

	private static String formatCursor(String generation, long sequence) {
		return CURSOR_PREFIX + generation + ":" + sequence;
	}

	private static LiveCursor parseCursor(String cursor) {
		if (cursor == null || cursor.isBlank())
			return null;
		try {
			if (!cursor.startsWith(CURSOR_PREFIX))
				return new LiveCursor(null, Long.parseLong(cursor)); // legacy cursor: necessarily discontinuous after upgrade
			String[] parts = cursor.substring(CURSOR_PREFIX.length()).split(":", 2);
			if (parts.length != 2 || parts[0].isBlank())
				throw new IllegalArgumentException("Invalid structured live log cursor: " + cursor);
			return new LiveCursor(parts[0], Long.parseLong(parts[1]));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid structured live log cursor: " + cursor, e);
		}
	}

	private record LiveCursor(String generation, long sequence) {
	}
}
