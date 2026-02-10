package hiconic.platform.reflex.cron.scheduling.wire.space;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import com.braintribe.exception.Exceptions;
import com.braintribe.logging.Logger;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.scope.InstanceConfiguration;

import hiconic.platform.reflex.cron.scheduling.api.CronScheduling;
import hiconic.platform.reflex.cron.scheduling.api.wire.contract.CronSchedulingContract;
import hiconic.platform.reflex.cron.scheduling.processing.CronSchedulingImpl;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class CronSchedulingRxModuleSpace implements RxModuleContract, CronSchedulingContract {
	private static final Logger logger = Logger.getLogger(CronSchedulingRxModuleSpace.class);

	@Import
	private RxPlatformContract platform;
	
	@Override
	@Managed
	public CronScheduling scheduling() {
		CronSchedulingImpl bean = new CronSchedulingImpl();
		bean.setScheduler(scheduler());
		bean.setSystemAttributeContextSupplier(platform.systemAttributeContextSupplier());
		bean.setSystemEvaluator(platform.systemEvaluator());
		return bean;
	}
	
	@Managed
	private Scheduler scheduler() {
		try {
			Scheduler bean = StdSchedulerFactory.getDefaultScheduler();

			InstanceConfiguration.currentInstance().onDestroy(() -> {
				try {
					bean.shutdown(true);
				} catch (SchedulerException e) {
					logger.error("error while shutting down standard quartz scheduler", e);
				}
			});

			bean.start();
			return bean;

		} catch (SchedulerException e) {
			throw Exceptions.unchecked(e, "Error while creating quartz standard scheduler");
		}
	}
}