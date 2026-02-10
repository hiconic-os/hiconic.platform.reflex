package hiconic.platform.reflex.cron.scheduling.processing;

import java.util.TimeZone;
import java.util.function.Supplier;

public record JobDeployment(String schedulingId, String crontab, TimeZone timeZone, Runnable job, boolean singleton, boolean coalescing) {

}
