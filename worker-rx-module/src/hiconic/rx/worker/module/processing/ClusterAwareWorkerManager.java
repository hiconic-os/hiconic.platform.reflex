// ============================================================================
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
package hiconic.rx.worker.module.processing;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.braintribe.cfg.DestructionAware;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.worker.api.Worker;
import com.braintribe.model.processing.worker.api.WorkerException;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.model.processing.worker.api.WorkerManagerControl;
import com.braintribe.utils.RandomTools;
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.rx.platform.processing.worker.BasicRxWorkerManager;
import hiconic.rx.platform.processing.worker.BasicRxWorkerManager.BasicWorkerContext;
import tribefire.cortex.leadership.api.LeadershipContext;
import tribefire.cortex.leadership.api.LeadershipListener;
import tribefire.cortex.leadership.api.LeadershipManager;

public class ClusterAwareWorkerManager implements WorkerManager, WorkerManagerControl, DestructionAware {

	private static final Logger logger = Logger.getLogger(ClusterAwareWorkerManager.class);

	private ExecutorService executorService;
	private final Map<Worker, ClusterAwareWorkerContext> workers = new ConcurrentHashMap<Worker, ClusterAwareWorkerContext>();

	private LazyInitialized<LeadershipManager> leadershipManager;

	private boolean started = false;
	private final Set<Worker> deferredWorkers = new LinkedHashSet<Worker>();
	private final Object deferLock = new Object();

	private String applicationId;

	@Required
	public void setLeadershipManagerSupplier(Supplier<LeadershipManager> leadershipManagerSupplier) {
		this.leadershipManager = new LazyInitialized<LeadershipManager>(leadershipManagerSupplier);
	}

	@Required
	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	@Required
	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}

	@Override
	public void start() throws WorkerException {
		synchronized (deferLock) {
			started = true;
			deployDeferredWorkers();
		}
	}

	@Override
	public void deploy(final Worker worker) throws WorkerException {
		synchronized (deferLock) {
			if (started)
				deployNow(worker);
			else
				deferredWorkers.add(worker);
		}
	}

	private void deployDeferredWorkers() {
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			for (Worker worker : deferredWorkers) {
				executor.submit(() -> {
					try {
						deployNow(worker);
					} catch (Exception e) {
						logger.error(String.format("Cannot deploy worker: [%s]", worker), e);
					}
				});
			}
		}

		deferredWorkers.clear();
	}

	public void deployNow(Worker worker) throws WorkerException {
		ClusterAwareWorkerContext workerInfo = new ClusterAwareWorkerContext();
		workerInfo.executorService = executorService;
		workerInfo.identification = BasicRxWorkerManager.buildWorkerIdentification(worker, applicationId);
		workerInfo.candidateId = RandomTools.newStandardUuid();
		workerInfo.worker = worker;
		workers.put(worker, workerInfo);

		logger.debug(() -> "Deploying worker: " + workerInfo);

		if (!worker.isSingleton()) {
			start(workerInfo);

		} else {
			LeadershipListener leadershipListener = new WorkerLeadershipListener(workerInfo);
			workerInfo.leadershipListener = leadershipListener;

			LeadershipManager leadershipManager = getLeadershipManager();
			logger.debug(() -> "Requesting leadership for singleton worker " + workerInfo + " with leadership manager " + leadershipManager);
			try {
				leadershipManager.addLeadershipListener(workerInfo.identification, leadershipListener);
			} catch (RuntimeException e) {
				workers.remove(worker);
				throw new WorkerException(String.format("failed when requesting leadership for singleton worker %s", workerInfo), e);
			}
		}
	}

	@Override
	public void preDestroy() {
		ClusterAwareWorkerContext infos[] = workers.values().toArray(new ClusterAwareWorkerContext[workers.values().size()]);
		for (ClusterAwareWorkerContext workerInfo : infos) {
			logger.warn("undeploying worker that was not stopped from externally: " + workerInfo);
			try {
				undeploy(workerInfo.worker);
			} catch (Exception e) {
				logger.warn("Error while automatically undeploying worker: " + workerInfo, e);
			}
		}
		started = false;
	}

	@Override
	public void undeploy(Worker worker) throws WorkerException {
		ClusterAwareWorkerContext workerInfo = workers.remove(worker);

		logger.debug(() -> "Undeploying worker: " + workerInfo);

		if (workerInfo == null) {
			logger.debug(() -> "No worker info available for worker " + worker);
			return;
		}

		synchronized (workerInfo) {
			stop(workerInfo);

			if (worker.isSingleton()) {
				try {
					getLeadershipManager().removeLeadershipListener(workerInfo.identification, workerInfo.leadershipListener);
				} catch (RuntimeException e) {
					throw new WorkerException("error while removing leadership listener for worker: " + workerInfo, e);
				}
			}
		}
	}

	private LeadershipManager getLeadershipManager() {
		return leadershipManager.get();
	}

	private class WorkerLeadershipListener implements LeadershipListener {

		private final ClusterAwareWorkerContext workerInfo;
		private boolean workerStarted = false;

		public WorkerLeadershipListener(ClusterAwareWorkerContext workerInfo) {
			this.workerInfo = workerInfo;
		}

		@Override
		public void surrenderLeadership(LeadershipContext context) {
			logger.debug(String.format("received request for surrender of worker: %s", workerInfo));
			workerStarted = false;
			stop(workerInfo);
		}

		@Override
		public void onLeadershipGranted(LeadershipContext context) {
			logger.debug(String.format("leadership was granted for worker %s", workerInfo));
			workerStarted = true;
			start(workerInfo);
		}

		@Override
		public String toString() {
			return "WorkerLeadershipCanidate (workerStarted: " + workerStarted + ") " + workerInfo.toString();
		}

	}

	protected void start(final ClusterAwareWorkerContext workerInfo) {
		synchronized (workerInfo) {
			logger.debug(() -> String.format("starting worker: %s", workerInfo));
			try {
				workerInfo.worker.start(workerInfo);
				logger.debug(() -> String.format("worker started: %s", workerInfo));
			} catch (WorkerException e) {
				logger.error(String.format("error in worker: %s", workerInfo), e);
			}
		}
	}

	protected void stop(ClusterAwareWorkerContext workerInfo) {
		synchronized (workerInfo) {
			Worker worker = workerInfo.worker;

			try {
				logger.debug(() -> String.format("stopping worker: %s", workerInfo));
				worker.stop(workerInfo);
			} catch (WorkerException e) {
				logger.error(String.format("error while stopping worker: %s", workerInfo));
			}
		}
	}

	public class ClusterAwareWorkerContext extends BasicWorkerContext {
		public LeadershipListener leadershipListener;
	}

}
