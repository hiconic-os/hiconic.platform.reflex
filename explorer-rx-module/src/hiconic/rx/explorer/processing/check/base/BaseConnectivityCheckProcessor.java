// ============================================================================
package hiconic.rx.explorer.processing.check.base;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.Numbers;
import com.braintribe.logging.Logger;
import com.braintribe.model.messaging.Message;
import com.braintribe.model.messaging.Topic;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.transport.messaging.api.MessageConsumer;
import com.braintribe.transport.messaging.api.MessageProducer;
import com.braintribe.transport.messaging.api.MessagingSession;
import com.braintribe.transport.messaging.api.MessagingSessionProvider;
import com.braintribe.utils.RandomTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.date.NanoClock;

import hiconic.rx.check.api.CheckProcessor;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;
import hiconic.rx.explorer.processing.platformreflection.db.DatabaseInformationProvider;
import hiconic.rx.explorer.processing.platformreflection.db.StandardDatabaseInformationProvider;
import hiconic.rx.reflection.model.db.DatabaseConnectionInfo;
import hiconic.rx.reflection.model.db.DatabaseConnectionPoolMetrics;
import hiconic.rx.reflection.model.db.DatabaseInformation;

public class BaseConnectivityCheckProcessor implements CheckProcessor {

	private static final Logger logger = Logger.getLogger(BaseConnectivityCheckProcessor.class);

	private ScheduledExecutorService scheduledExecutorService;
	private DatabaseInformationProvider databaseInformationProvider;

	private Supplier<MessagingSessionProvider> messagingSessionProviderSupplier;
	private String messagingTestTopicName = "system-health-check";
	private long messagingWaitTime = 3000L;

	@Override
	public CheckResult check(ServiceRequestContext requestContext) {
		CheckResult response = CheckResult.T.create();

		Map<String, Future<List<CheckResultEntry>>> futures = new HashMap<>();
		futures.put("Connection Pools", scheduledExecutorService.submit(this::checkConnectionPools));
		futures.put("Messaging", scheduledExecutorService.submit(this::checkMessaging));

		long waitTill = System.currentTimeMillis() + 10_000;
		for (Map.Entry<String, Future<List<CheckResultEntry>>> futureSet : futures.entrySet()) {
			String type = futureSet.getKey();
			Future<List<CheckResultEntry>> future = futureSet.getValue();

			try {
				long waitFor = Math.max(10,  waitTill - System.currentTimeMillis());

				List<CheckResultEntry> list = future.get(waitFor, TimeUnit.MILLISECONDS);
				response.getEntries().addAll(list);

			} catch (InterruptedException ie) {
				logger.debug(() -> "Got interrupted while waiting for check results.");
				break;

			} catch (TimeoutException te) {
				logger.warn(() -> "Got a timeout while waiting for check results of " + type);
				response.getEntries().add( //
						newEntry(type, "Timed out within 10 seconds.", CheckStatus.warn));

			} catch (Exception e) {
				logger.warn(() -> "Got an error while getting result for " + type, e);
				response.getEntries().add( //
						newEntry(type, "Exception: " + e.getMessage(), CheckStatus.fail));
			}
		}

		return response;

	}

	private CheckResultEntry newEntry(String type, String description, CheckStatus status) {
		CheckResultEntry entry = CheckResultEntry.T.create();
		entry.setName("Type: " + type);
		entry.setDetails(description);
		entry.setCheckStatus(status);

		return entry;
	}

	private List<CheckResultEntry> checkMessaging() {

		List<CheckResultEntry> result = new ArrayList<>();
		if (messagingSessionProviderSupplier == null) {
			logger.debug(() -> "Got no messaging session provider supplier");
			return result;
		}
		MessagingSessionProvider messagingSessionProvider = messagingSessionProviderSupplier.get();
		if (messagingSessionProvider == null) {
			logger.debug(() -> "Got a messaging session provider supplier, but no messaging session provider");
			return result;
		}

		CheckResultEntry entry = CheckResultEntry.T.create();
		entry.setName("Messaging");
		entry.setDetailsAsMarkdown(true);
		result.add(entry);

		Topic testTopic = Topic.T.create();
		testTopic.setName(messagingTestTopicName);

		MessagingSession messagingSession = null;
		MessageConsumer consumer = null;
		MessageProducer createMessageProducer = null;
		ReentrantLock lock = new ReentrantLock();
		Condition newMessageArrived = lock.newCondition();
		try {
			String correlationId = RandomTools.newStandardUuid();
			Message message = Message.T.create();
			message.setBody("Health check with ID " + correlationId);
			message.setCorrelationId(correlationId);
			message.setDestination(testTopic);
			message.setMessageId(correlationId);
			message.setTimeToLive((long) Numbers.MILLISECONDS_PER_MINUTE);

			messagingSession = messagingSessionProvider.provideMessagingSession();

			ConcurrentHashMap<String, Instant> messages = new ConcurrentHashMap<>();
			consumer = messagingSession.createMessageConsumer(testTopic);
			consumer.setMessageListener(m -> {
				String id = m.getCorrelationId();
				messages.put(id, NanoClock.INSTANCE.instant());
				lock.lock();
				try {
					newMessageArrived.signal();
				} finally {
					lock.unlock();
				}
			});

			Instant messageSentInstant = NanoClock.INSTANCE.instant();
			Duration messageTransitDuration = null;

			createMessageProducer = messagingSession.createMessageProducer(testTopic);
			createMessageProducer.sendMessage(message);

			long start = System.currentTimeMillis();
			long end = start + messagingWaitTime;
			long now = start;
			boolean success = false;
			while (now < end) {
				Instant receivedInstant = messages.get(correlationId);
				if (receivedInstant != null) {
					messageTransitDuration = Duration.between(messageSentInstant, receivedInstant);
					if (logger.isDebugEnabled())
						logger.debug("Received own check message after " + StringTools.prettyPrintDuration(messageTransitDuration, true, null));
					success = true;
					break;
				}
				lock.lock();
				try {
					newMessageArrived.await(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					logger.debug(() -> "Got interrupted while waiting for the health check message");
					break;
				} finally {
					lock.unlock();
				}
				now = System.currentTimeMillis();
			}

			String desc = messagingSessionProvider.description();

			StringBuilder sb = new StringBuilder();
			sb.append("Name | Message Transit Duration\n");
			sb.append("--- | ---\n");
			sb.append(desc + " | ");
			sb.append(StringTools.prettyPrintDuration(messageTransitDuration, true, null) + "\n");
			entry.setDetails(sb.toString());
			entry.setCheckStatus(success ? CheckStatus.ok : CheckStatus.fail);

		} catch (Exception e) {
			logger.error("Error while trying to send a message via " + messagingSessionProviderSupplier, e);

			String details = messagingSessionProvider.description();
			entry.setMessage(details + ": " + e.getMessage());
			entry.setCheckStatus(CheckStatus.fail);

		} finally {
			try {
				if (createMessageProducer != null)
					createMessageProducer.close();

				if (consumer != null)
					consumer.close();

				if (messagingSession != null)
					messagingSession.close();

			} catch (Exception e) {
				logger.debug(() -> "Error while closing messaging.", e);
			}
		}

		return result;
	}

	private List<CheckResultEntry> checkConnectionPools() {
		List<CheckResultEntry> result = new ArrayList<>();

		DatabaseInformation databaseInformation = getDatabaseInformationProvider().get();
		if (databaseInformation == null)
			return result;

		List<DatabaseConnectionInfo> connectionPools = databaseInformation.getConnectionPools();
		if (connectionPools.isEmpty())
			return result;

		CheckResultEntry entry = CheckResultEntry.T.create();
		entry.setName("Connection Pools");
		entry.setCheckStatus(CheckStatus.ok);
		entry.setDetailsAsMarkdown(true);
		result.add(entry);

		StringBuilder sb = new StringBuilder();
		sb.append("Name | Idle Connections | Threads Waiting | Active Connections | Total Connections | Max Pool Size\n");
		sb.append("--- | --- | --- | --- | --- | ---\n");

		for (DatabaseConnectionInfo cp : connectionPools) {
			DatabaseConnectionPoolMetrics metrics = cp.getMetrics();

			String idleConnections = getNumberOrDash(metrics, DatabaseConnectionPoolMetrics::getIdleConnections);
			String awaiting = getNumberOrDash(metrics, DatabaseConnectionPoolMetrics::getThreadsAwaitingConnections);
			String active = getNumberOrDash(metrics, DatabaseConnectionPoolMetrics::getActiveConnections);
			String total = getNumberOrDash(metrics, DatabaseConnectionPoolMetrics::getTotalConnections);

			Integer maxPoolSize = cp.getMaximumPoolSize();
			if (maxPoolSize == null)
				maxPoolSize = 0;

			if (metrics != null && metrics.getThreadsAwaitingConnections() > 5) {
				logger.debug(() -> "There are " + awaiting + " threads waiting for a connection.");
				// Note: this used to trigger a warning, but it might be legit that there are way more processes
				// than connections in the pool
				entry.setCheckStatus(CheckStatus.ok);

			} else {
				entry.setCheckStatus(CheckStatus.ok);
			}

			sb.append(cp.getName() + "|");
			sb.append(idleConnections + "|");
			sb.append(awaiting + "|");
			sb.append(active + "|");
			sb.append(total + "|");
			sb.append(maxPoolSize + "\n");

		}
		entry.setDetails(sb.toString());
		return result;
	}

	private String getNumberOrDash(DatabaseConnectionPoolMetrics metrics, Function<DatabaseConnectionPoolMetrics, Integer> getter) {
		if (metrics == null)
			return "-";

		Integer value = getter.apply(metrics);
		if (value == null)
			return "0";

		return value.toString();
	}

	@Required
	public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
		this.scheduledExecutorService = scheduledExecutorService;
	}
	public DatabaseInformationProvider getDatabaseInformationProvider() {
		if (databaseInformationProvider == null) {
			databaseInformationProvider = new StandardDatabaseInformationProvider();
		}
		return databaseInformationProvider;
	}
	@Configurable
	public void setDatabaseInformationProvider(DatabaseInformationProvider databaseInformationProvider) {
		this.databaseInformationProvider = databaseInformationProvider;
	}
	@Configurable
	@Required
	public void setMessagingSessionProviderSupplier(Supplier<MessagingSessionProvider> messagingSessionProvider) {
		this.messagingSessionProviderSupplier = messagingSessionProvider;
	}
	@Configurable
	public void setMessagingTestTopicName(String messagingTestTopicName) {
		this.messagingTestTopicName = messagingTestTopicName;
	}
	@Configurable
	public void setMessagingWaitTime(long messagingWaitTime) {
		this.messagingWaitTime = messagingWaitTime;
	}

}
