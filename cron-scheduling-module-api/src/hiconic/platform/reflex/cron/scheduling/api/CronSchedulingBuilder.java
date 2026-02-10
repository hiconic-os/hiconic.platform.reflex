package hiconic.platform.reflex.cron.scheduling.api;

import java.time.ZoneId;
import java.util.TimeZone;
import java.util.function.Supplier;

import com.braintribe.model.service.api.ServiceRequest;

public interface CronSchedulingBuilder {
	CronSchedulingBuilder withId(String id);
	CronSchedulingBuilder coalescing();
	CronSchedulingBuilder singleton();
	CronSchedulingBuilder inTimeZone(String zoneId);
	CronSchedulingBuilder inTimeZone(ZoneId timeZone);
	CronSchedulingBuilder inTimeZone(TimeZone timeZone);
	void schedule(Supplier<Runnable> jobSupplier);
	void schedule(Runnable job);
	void schedule(ServiceRequest request);
}
