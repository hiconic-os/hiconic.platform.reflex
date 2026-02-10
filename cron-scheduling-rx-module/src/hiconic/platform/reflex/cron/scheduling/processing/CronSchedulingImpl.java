package hiconic.platform.reflex.cron.scheduling.processing;

import java.time.ZoneId;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Supplier;

import org.quartz.Scheduler;

import com.braintribe.cfg.Required;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.platform.reflex.cron.scheduling.api.CronScheduling;
import hiconic.platform.reflex.cron.scheduling.api.CronSchedulingBuilder;

public class CronSchedulingImpl implements CronScheduling, CronSchedulingConfig {
	private WorkerManager workerManager;
	private Supplier<AttributeContext> systemAttributeContextSupplier;
	private Scheduler scheduler;
	private Evaluator<ServiceRequest> systemEvaluator;

	@Required
	public void setSystemEvaluator(Evaluator<ServiceRequest> systemEvaluator) {
		this.systemEvaluator = systemEvaluator;
	}
	
	@Required
	public void setSystemAttributeContextSupplier(Supplier<AttributeContext> systemAttributeContextSupplier) {
		this.systemAttributeContextSupplier = systemAttributeContextSupplier;
	}
	
	@Required
	public void setWorkerManager(WorkerManager workerManager) {
		this.workerManager = workerManager;
	}
	
	@Required
	public void setScheduler(Scheduler scheduler) {
		this.scheduler = scheduler;
	}
	
	@Override
	public Scheduler scheduler() {
		return scheduler;
	}
	
	@Override
	public CronSchedulingBuilder build(String crontab) {
		return new CronSchedulingBuilder() {
			TimeZone timezone = TimeZone.getDefault();
			String id;
			boolean coalescing = false;
			boolean singleton = false;
			
			@Override
			public CronSchedulingBuilder coalescing() {
				coalescing = true;
				return this;
			}
			
			@Override
			public CronSchedulingBuilder singleton() {
				singleton = true;
				return this;
			}
			
			@Override
			public CronSchedulingBuilder withId(String id) {
				this.id = id;
				return this;
			}
			
			@Override
			public void schedule(Runnable job) {
				schedule(() -> job);
			}
			
			@Override
			public void schedule(Supplier<Runnable> jobSupplier) {
				scheduleAuthenticated(systemUserRunnable(jobSupplier));
			}
			
			@Override
			public void schedule(ServiceRequest request) {
				scheduleAuthenticated(requestEvaluationRunnable(request));
			}
			
			private void scheduleAuthenticated(Runnable job) {
				String schedulingId = id != null? id: UUID.randomUUID().toString();
				JobDeployment deployment = new JobDeployment(
						schedulingId, 
						crontab, 
						timezone, 
						job, 
						singleton, 
						coalescing);
				CronSchedulingWorker worker = new CronSchedulingWorker(CronSchedulingImpl.this, deployment);
				workerManager.deploy(worker);
			}
			
			@Override
			public CronSchedulingBuilder inTimeZone(TimeZone timeZone) {
				this.timezone = timeZone;
				return this;
			}
			
			@Override
			public CronSchedulingBuilder inTimeZone(String zoneId) {
				return inTimeZone(TimeZone.getTimeZone(zoneId));
			}
			
			@Override
			public CronSchedulingBuilder inTimeZone(ZoneId timeZone) {
				return inTimeZone(TimeZone.getTimeZone(timeZone));
			}
		};
	}
	
	private Runnable requestEvaluationRunnable(ServiceRequest request) {
		return () -> systemEvaluator.eval(request).get(); 
	}
	
	private Runnable systemUserRunnable(Supplier<Runnable> jobSupplier) {
		return () -> AttributeContexts.with(systemAttributeContextSupplier.get()).run(jobSupplier.get());
	}
}
