package hiconic.platform.reflex.cron.scheduling.processing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;

import com.braintribe.logging.Logger;
import com.braintribe.model.processing.worker.api.Worker;
import com.braintribe.model.processing.worker.api.WorkerContext;
import com.braintribe.model.processing.worker.api.WorkerException;
import com.braintribe.utils.collection.impl.AttributeContexts;

public class CronSchedulingWorker implements Worker {

	private static Logger logger = Logger.getLogger(CronSchedulingWorker.class);

	private CronTrigger trigger;
	private static Map<JobKey, JobImpl> jobs = new ConcurrentHashMap<>();

	private CronSchedulingConfig config;
	private JobDeployment deployment;

	public CronSchedulingWorker(CronSchedulingConfig config, JobDeployment deployment) {
		this.deployment = deployment;
		this.config = config;
	}
	
	@Override
	public void start(WorkerContext workerContext) throws WorkerException {

		try {
			JobImpl job = new JobImpl();
			JobKey key = new JobKey(deployment.schedulingId());

			jobs.put(key, job);
			JobDetail jobDetail = JobBuilder.newJob().ofType(DispatcherJob.class).withIdentity(key).build();

			CronScheduleBuilder cronSchedule = CronScheduleBuilder.cronSchedule(deployment.crontab())
					.inTimeZone(deployment.timeZone());

			trigger = TriggerBuilder.newTrigger().withSchedule(cronSchedule).forJob(jobDetail).build();

			logger.info("Scheduling job: " + deployment);
			config.scheduler().scheduleJob(jobDetail, trigger);

		} catch (SchedulerException e) {
			trigger = null;
			throw new WorkerException("Error while scheduling job: " + deployment, e);
		}

	}

	@Override
	public void stop(WorkerContext workerContext) throws WorkerException {
		if (trigger != null) {
			try {
				JobKey key = trigger.getJobKey();
				config.scheduler().unscheduleJob(trigger.getKey());
				JobImpl job = jobs.remove(key);
				
				if (job != null) {
					logger.debug(
							() -> "Interrupting and waiting for job scheduling with key " + key + ". Deployable is: " + deployment);
					
					job.interruptAndWaitForEnd();
				} else {
					logger.debug(() -> "Could not find a job with key " + key + ". Deployable is: " + deployment);
				}
				
			} catch (SchedulerException e) {
				throw new WorkerException("Error while unscheduling job: " + deployment);
			}
		}
	}
	
	@Override
	public boolean isSingleton() {
		return deployment.singleton();
	}
	
	private class JobImpl implements Job {

		private Object monitor = new Object();
		private volatile int runCount = 0;
		private boolean interrupted;

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			synchronized (monitor) {
				if (interrupted || runCount > 0 && deployment.coalescing()) {
					return;
				}

				runCount++;
			}

			try {
				logger.info("Starting scheduled job: " + deployment);
				deployment.job().run();
			} catch (Exception e) {
				logger.error("Error while executing scheduled job: " + deployment);

			} finally {
				synchronized (monitor) {
					runCount--;
					monitor.notify();
				}
			}
		}

		public void interruptAndWaitForEnd() {
			synchronized (monitor) {
				interrupted = true;

				while (runCount > 0) {
					try {
						monitor.wait();
					} catch (InterruptedException e) {
						// intentionally left empty
					}
				}
			}
		}
	}

	public static class DispatcherJob implements Job {

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			Job job = jobs.get(context.getJobDetail().getKey());
			job.execute(context);
		}
	}
}
