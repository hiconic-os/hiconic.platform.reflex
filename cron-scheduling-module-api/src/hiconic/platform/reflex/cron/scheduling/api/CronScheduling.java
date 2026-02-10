package hiconic.platform.reflex.cron.scheduling.api;

public interface CronScheduling {
	CronSchedulingBuilder build(String crontab);
}
