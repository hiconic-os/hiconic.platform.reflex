package hiconic.platform.reflex.cron.scheduling.processing;

import org.quartz.Scheduler;

public interface CronSchedulingConfig {
	Scheduler scheduler();
}
