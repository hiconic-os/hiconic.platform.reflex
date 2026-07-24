package hiconic.rx.log.reflection.processing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
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
 * Captures Logback events before an encoder removes structural information. The collector never logs and never blocks producers on I/O.
 */
public class StructuredLiveLogCollector implements LifecycleAware {
	public static final String STREAM_ID = "structured-live";

	private final Object monitor = new Object();
	private final ArrayDeque<LogRecord> records = new ArrayDeque<>();
	private final AtomicLong sequence = new AtomicLong();
	private final List<Logger> attachedLoggers = new ArrayList<>();

	private InstanceId instanceId;
	private LoggerContext loggerContext;
	private int capacity = 10_000;
	private AppenderBase<ILoggingEvent> appender;

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

	@Override
	public void postConstruct() {
		if (loggerContext == null)
			loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		appender = new AppenderBase<>() {
			@Override
			protected void append(ILoggingEvent event) {
				capture(event);
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

	@Override
	public void preDestroy() {
		for (Logger logger : attachedLoggers)
			logger.detachAppender(appender);
		attachedLoggers.clear();
		if (appender != null)
			appender.stop();
	}

	private void attach(Logger logger) {
		logger.addAppender(appender);
		attachedLoggers.add(logger);
	}

	private void capture(ILoggingEvent event) {
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

		LogPosition position = LogPosition.T.create();
		position.setSequence(sequence.incrementAndGet());
		record.setPosition(position);

		synchronized (monitor) {
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
		int limit = Math.max(1, Math.min(requestedLimit, 10_000));
		Long afterSequence = parseCursor(cursor);

		List<LogRecord> matches;
		synchronized (monitor) {
			matches = matches(filter, afterSequence);
			if (matches.isEmpty() && afterSequence != null && waitMillis > 0) {
				try {
					monitor.wait(Math.min(waitMillis, 30_000));
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				matches = matches(filter, afterSequence);
			}
		}

		boolean moreAvailable = matches.size() > limit;
		List<LogRecord> page;
		if (afterSequence == null && matches.size() > limit)
			page = new ArrayList<>(matches.subList(matches.size() - limit, matches.size()));
		else
			page = new ArrayList<>(matches.subList(0, Math.min(limit, matches.size())));

		LogRecordPage response = LogRecordPage.T.create();
		response.setRecords(page);
		response.setMoreAvailable(moreAvailable);
		response.setObservedProperties(LogRecordFiltering.observedProperties(page));
		if (!page.isEmpty())
			response.setNextCursor(Long.toString(page.get(page.size() - 1).getPosition().getSequence()));
		else
			response.setNextCursor(cursor);
		return response;
	}

	private List<LogRecord> matches(LogFilter filter, Long afterSequence) {
		List<LogRecord> result = new ArrayList<>();
		for (LogRecord record : records) {
			if (afterSequence != null && record.getPosition().getSequence() <= afterSequence)
				continue;
			if (LogRecordFiltering.matches(record, filter))
				result.add(record);
		}
		return result;
	}

	private static Long parseCursor(String cursor) {
		if (cursor == null || cursor.isBlank())
			return null;
		try {
			return Long.valueOf(cursor);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid structured live log cursor: " + cursor, e);
		}
	}
}
