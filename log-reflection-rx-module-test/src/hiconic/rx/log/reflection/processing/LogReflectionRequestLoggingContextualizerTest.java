package hiconic.rx.log.reflection.processing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.braintribe.model.processing.service.api.ServiceRequestExecution;

import hiconic.rx.log.reflection.model.api.ListLogStreams;
import hiconic.rx.log.reflection.model.api.QueryLocalLogRecords;
import hiconic.rx.log.reflection.model.api.QueryLogRecords;
import hiconic.rx.module.api.log.RxLogManager;

public class LogReflectionRequestLoggingContextualizerTest {

	@Test
	public void suppressesOnlyLongPollingPublicAndClusterRequests() throws Exception {
		CountingLogManager logManager = new CountingLogManager();
		LogReflectionRequestLoggingContextualizer contextualizer = new LogReflectionRequestLoggingContextualizer(logManager);

		QueryLogRecords publicTail = QueryLogRecords.T.create();
		publicTail.setWaitMillis(1);
		contextualizer.contextualize(publicTail, () -> logManager.executions++);

		QueryLocalLogRecords clusterTail = QueryLocalLogRecords.T.create();
		clusterTail.setWaitMillis(1);
		contextualizer.contextualize(clusterTail, () -> logManager.executions++);

		QueryLogRecords immediateQuery = QueryLogRecords.T.create();
		contextualizer.contextualize(immediateQuery, () -> logManager.executions++);
		contextualizer.contextualize(ListLogStreams.T.create(), () -> logManager.executions++);

		assertThat(logManager.suppressions).isEqualTo(2);
		assertThat(logManager.executions).isEqualTo(4);
	}

	private static class CountingLogManager implements RxLogManager {
		private int suppressions;
		private int executions;

		@Override public void setLogLevel(String logger, System.Logger.Level level) { /* not relevant */ }
		@Override public void setLogLevel(Class<?> clazz, System.Logger.Level level) { /* not relevant */ }

		@Override
		public void suppressRoutineLogging(ServiceRequestExecution execution) throws Exception {
			suppressions++;
			execution.execute();
		}
	}
}
