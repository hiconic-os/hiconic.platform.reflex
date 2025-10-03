// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.explorer.processing.check.base;

import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.LifecycleAware;
import com.braintribe.cfg.Required;
import com.braintribe.exception.Exceptions;
import com.braintribe.execution.monitoring.ThreadPoolMonitoring;
import com.braintribe.execution.monitoring.ThreadPoolStatistics;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.lock.api.Locking;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.CollectionTools2;

import hiconic.rx.check.api.CheckProcessor;
import hiconic.rx.check.model.result.CheckResult;
import hiconic.rx.check.model.result.CheckResultEntry;
import hiconic.rx.check.model.result.CheckStatus;
import tribefire.cortex.leadership.api.LeadershipContext;
import tribefire.cortex.leadership.api.LeadershipListener;
import tribefire.cortex.leadership.api.LeadershipManager;

public class BaseFunctionalityCheckProcessor implements CheckProcessor, LifecycleAware {

	private static final Logger logger = Logger.getLogger(BaseFunctionalityCheckProcessor.class);

	private Locking locking;
	private LeadershipManager leadershipManager;

	private CheckResultEntry lockCheckResultEntry = null;
	private CheckResultEntry leadershipCheckResultEntry = null;
	private ScheduledExecutorService scheduledExecutorService;
	private Future<?> checkFuture;

	protected Evaluator<ServiceRequest> requestEvaluator;

	@Override
	public CheckResult check(ServiceRequestContext requestContext) {

		CheckResult response = CheckResult.T.create();

		if (lockCheckResultEntry != null) {
			response.getEntries().add(lockCheckResultEntry);
		}
		if (leadershipCheckResultEntry != null) {
			response.getEntries().add(leadershipCheckResultEntry);
		}
		checkFuture = scheduledExecutorService.submit(this::performConcurrencyChecks);

		Map<String, Future<List<CheckResultEntry>>> futures = new HashMap<>();
		futures.put("Thread Pool", scheduledExecutorService.submit(this::checkThreadPools));

		for (Map.Entry<String, Future<List<CheckResultEntry>>> futureSet : futures.entrySet()) {
			String type = futureSet.getKey();
			Future<List<CheckResultEntry>> future = futureSet.getValue();

			try {
				List<CheckResultEntry> list = future.get(10, TimeUnit.SECONDS);
				if (list != null) {
					response.getEntries().addAll(list);
				}
			} catch (InterruptedException ie) {
				logger.debug(() -> "Got interrupted while waiting for check results.");
				break;
			} catch (TimeoutException te) {
				logger.warn(() -> "Got a timeout while waiting for check results of " + type);
			} catch (Exception e) {
				logger.warn(() -> "Got an error while getting result for " + type, e);
			}
		}

		return response;

	}

	private List<CheckResultEntry> checkThreadPools() {

		List<ThreadPoolStatistics> statistics = ThreadPoolMonitoring.getStatistics();

		if (statistics != null) {

			CheckResultEntry result = CheckResultEntry.T.create();
			result.setCheckStatus(CheckStatus.ok);
			result.setName("Thread Pools");

			TreeMap<String, String> sortedEntries = new TreeMap<>();

			for (ThreadPoolStatistics stats : statistics) {

				String name = stats.getDescription();
				if (name == null) {
					name = "n/a";
				}

				int pending = stats.getPendingTasksInQueue();

				StringBuilder sb = new StringBuilder();

				sb.append(name + "|");
				sb.append(stats.currentlyRunning() + "|");
				sb.append(stats.totalExecutions() + "|");
				sb.append(stats.averageRunningTimeInMs() + " ms|");
				sb.append(stats.getPoolSize() + "|");
				sb.append(stats.getCorePoolSize() + "|");
				int maximumPoolSize = stats.getMaximumPoolSize();
				if (maximumPoolSize == Integer.MAX_VALUE) {
					sb.append("unbound|");
				} else {
					sb.append(maximumPoolSize + "|");
				}
				sb.append(pending + "|");
				Double averageEnqueuedTimeInMs = stats.getAverageEnqueuedTimeInMs();
				if (averageEnqueuedTimeInMs != null) {
					sb.append(StringTools.prettyPrintMilliseconds(averageEnqueuedTimeInMs, true, ChronoUnit.NANOS) + "|");
				} else {
					sb.append(" |");
				}
				long timeSinceLastExecution = stats.timeSinceLastExecutionInMs();
				String timeSinceLastExecutionString = StringTools.prettyPrintMilliseconds(timeSinceLastExecution, true, ChronoUnit.MILLIS);
				int index = timeSinceLastExecutionString.indexOf(" ");
				if (index != -1) {
					index = timeSinceLastExecutionString.indexOf(" ", index + 1);
					if (index != -1) {
						timeSinceLastExecutionString = timeSinceLastExecutionString.substring(0, index);
					}
				}
				sb.append(timeSinceLastExecutionString);
				sb.append("\n");

				sortedEntries.put(name, sb.toString());
			}

			StringBuilder sb = new StringBuilder();
			sb.append(
					"Name|Active Threads|Total Executions|Average Execution Time|Pool Size|Core Pool Size|Maximum Pool Size|Pending Tasks in Queue|Average Pending Time in Queue|Time Since Last Execution\n");
			sb.append("---|---|---|---|---|---|---|---|---|---\n");

			sortedEntries.values().forEach(l -> sb.append(l));
			result.setDetails(sb.toString());
			result.setDetailsAsMarkdown(true);
			return CollectionTools2.asList(result);

		}

		return Collections.EMPTY_LIST;
	}

	private void performConcurrencyChecks() {
		checkLockManager();
		checkLeadershipManager();
	}

	protected void checkLockManager() {
		if (locking == null) {
			return;
		}

		CheckResultEntry entry = CheckResultEntry.T.create();
		entry.setName("Lock Check");
		entry.setDetailsAsMarkdown(true);
		StringBuilder sb = new StringBuilder();
		sb.append("---|---\n");
		sb.append("Locking|" + locking.getClass().getSimpleName() + "\n");
		entry.setDetails(sb.toString());

		try {
			String identifier = BaseFunctionalityCheckProcessor.class.getName();

			Lock lock = locking.forIdentifier(identifier).writeLock();
			lock.lock();
			try {
				logger.debug(() -> "Lock acquired. Locking: " + locking);
			} finally {
				lock.unlock();
			}

			entry.setCheckStatus(CheckStatus.ok);

		} catch (Exception e) {
			logger.error("Error while trying to check the lock manager.", e);
			entry.setCheckStatus(CheckStatus.fail);
			entry.setMessage(Exceptions.getRootCause(e).getMessage());
		}

		lockCheckResultEntry = entry;
	}

	protected void checkLeadershipManager() {

		if (leadershipManager == null) {
			return;
		}

		CheckResultEntry entry = CheckResultEntry.T.create();
		entry.setName("Leadership Check");
		entry.setDetailsAsMarkdown(true);
		StringBuilder sb = new StringBuilder();
		sb.append("---|---\n");
		sb.append("Leadership Manager|" + leadershipManager.description() + "\n");
		entry.setDetails(sb.toString());

		String domainId = "base-health-check";
		
		LeadershipListener4Check leadershipListener = new LeadershipListener4Check();
		try {

			leadershipManager.addLeadershipListener(domainId, leadershipListener);
			boolean success = leadershipListener.latch.await(1, TimeUnit.MINUTES);

			if (success) {
				entry.setCheckStatus(CheckStatus.ok);
			} else {
				entry.setCheckStatus(CheckStatus.fail);
				entry.setMessage(
						"Leadership Manager (" + leadershipManager.description() + ") did not assign leadership within the required timeframe");
			}

		} catch (Exception e) {
			logger.error("Error while trying to check the leadership manager.", e);
			entry.setCheckStatus(CheckStatus.fail);
			entry.setMessage(Exceptions.getRootCause(e).getMessage());
		} finally {
			try {
				leadershipManager.removeLeadershipListener(domainId, leadershipListener);
			} catch (Exception e) {
				logger.error("Error while trying to remove the leadership listener.", e);
				if (entry.getCheckStatus() == CheckStatus.ok) {
					entry.setCheckStatus(CheckStatus.fail);
					entry.setMessage(Exceptions.getRootCause(e).getMessage());
				}
			}
		}

		leadershipCheckResultEntry = entry;
	}

	class LeadershipListener4Check implements LeadershipListener {
		CountDownLatch latch = new CountDownLatch(1);

		@Override
		public void onLeadershipGranted(LeadershipContext context) {
			logger.trace(() -> "Got leadership");
			latch.countDown();
		}

		@Override
		public void surrenderLeadership(LeadershipContext context) {
			logger.trace(() -> "Was ordered to surrender leadership");
		}
	}

	@Configurable
	public void setLocking(Locking locking) {
		this.locking = locking;
	}

	@Configurable
	public void setLeadershipManager(LeadershipManager leadershipManager) {
		this.leadershipManager = leadershipManager;
	}

	@Override
	public void postConstruct() {
		checkFuture = scheduledExecutorService.schedule(this::performConcurrencyChecks, 10, TimeUnit.SECONDS);
	}

	@Override
	public void preDestroy() {
		if (checkFuture != null) {
			checkFuture.cancel(true);
		}
	}

	@Required
	public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
		this.scheduledExecutorService = scheduledExecutorService;
	}

	@Configurable
	@Required
	public void setRequestEvaluator(Evaluator<ServiceRequest> requestEvaluator) {
		this.requestEvaluator = requestEvaluator;
	}

}
