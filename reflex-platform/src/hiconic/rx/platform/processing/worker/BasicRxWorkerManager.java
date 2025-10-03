// ============================================================================
package hiconic.rx.platform.processing.worker;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.braintribe.cfg.DestructionAware;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.core.commons.IdentificationBuilders;
import com.braintribe.model.processing.securityservice.api.UserSessionScoping;
import com.braintribe.model.processing.securityservice.api.exceptions.SecurityServiceException;
import com.braintribe.model.processing.worker.api.Worker;
import com.braintribe.model.processing.worker.api.WorkerContext;
import com.braintribe.model.processing.worker.api.WorkerException;
import com.braintribe.model.processing.worker.api.WorkerExecutionContext;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.model.processing.worker.api.WorkerManagerControl;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.RandomTools;

public class BasicRxWorkerManager implements WorkerManager, WorkerManagerControl, DestructionAware {

	private static final Logger logger = Logger.getLogger(BasicRxWorkerManager.class);

	private ExecutorService executorService;
	private final Map<Worker, BasicWorkerContext> workers = new ConcurrentHashMap<Worker, BasicWorkerContext>();

	private boolean started = false;
	private final Set<Worker> deferredWorkers = new LinkedHashSet<Worker>();
	private final Object deferLock = new Object();

	private String applicationId;

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
						logger.error(String.format("cannot deploy worker [%s]", worker), e);
					}
				});
			}
		}

		deferredWorkers.clear();
	}

	public void deployNow(Worker worker) throws WorkerException {
		BasicWorkerContext workerInfo = new BasicWorkerContext();
		workerInfo.executorService = executorService;
		workerInfo.identification = buildWorkerIdentification(worker, applicationId);
		workerInfo.candidateId = RandomTools.newStandardUuid();
		workerInfo.worker = worker;
		workers.put(worker, workerInfo);

		logger.debug(() -> "Deploying worker: " + workerInfo);

		start(workerInfo);
	}

	public static String buildWorkerIdentification(Worker worker, String applicationId) {
		return IdentificationBuilders.//
				fromInstance(worker.getWorkerIdentification()) //
				.outerNamespace(applicationId) //
				.namespaceFromType() //
				.build();
	}

	@Override
	public void preDestroy() {
		BasicWorkerContext infos[] = workers.values().toArray(new BasicWorkerContext[workers.values().size()]);
		for (BasicWorkerContext workerInfo : infos) {
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
		BasicWorkerContext workerInfo = workers.remove(worker);

		logger.debug(() -> "Undeploying worker: " + workerInfo);

		if (workerInfo == null) {
			logger.debug(() -> "No worker info available for worker " + worker);
			return;
		}

		synchronized (workerInfo) {
			stop(workerInfo);
		}
	}

	protected void start(final BasicWorkerContext workerInfo) {
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

	protected void stop(BasicWorkerContext workerInfo) {
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

	public static class BasicWorkerContext implements WorkerContext {
		public ExecutorService executorService;
		public String candidateId;
		public String identification;
		public Worker worker;
		public Object customValue;

		@Override
		public String toString() {
			return String.format("%s[identification=%s,candidateId=%s]", worker.getClass().getSimpleName(), identification, candidateId);
		}

		@Override
		public Future<?> submit(Runnable task) {
			Callable<?> callable = () -> {
				task.run();
				return null;
			};
			return submit(callable);
		}

		@Override
		public <T> Future<T> submit(Callable<T> task) {
			return BasicRxWorkerManager.submit(task, this, executorService);
		}

		@Override
		@Deprecated
		public UserSessionScoping getUserSessionScoping() {
			throw new UnsupportedOperationException("Method 'WorkerContext.getUserSessionScoping' is not supported!");
		}

		@Override
		@Deprecated
		public UserSession getSystemUserSession() throws WorkerException {
			throw new UnsupportedOperationException("Method 'WorkerContext.getSystemUserSession' is not supported!");
		}
	}

	public static <T> Future<T> submit(Callable<T> task, WorkerContext wc, ExecutorService executorService) {
		logger.debug(() -> String.format("worker submitted task for %s: %s", wc, task));

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		return executorService.submit(() -> {
			logger.debug(() -> String.format("Worker %s started task: %s", wc, task));
			ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(classLoader);
				WorkerExecutionContext wctx = new WorkerExecutionContext("Worker", task);
				wctx.push();

				try {
					T returnValue = task.call();
					logger.debug(() -> String.format("Worker %s finished task: %s", wc, task));
					return returnValue;

				} catch (Throwable e) {
					logger.error(String.format("Error while worker %s was executing task: %s", wc, task), e);

					if (e instanceof RuntimeException) {
						throw (RuntimeException) e;
					} else {
						throw e;
					}

				} finally {
					Thread.currentThread().setContextClassLoader(origClassLoader);
					wctx.pop();
				}

			} catch (SecurityServiceException e) {
				throw new WorkerException(String.format("error when pushing default user session for task of worker %s: %s", wc, task), e);
			}
		});
	}
}
