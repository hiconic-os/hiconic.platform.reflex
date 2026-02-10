package hiconic.platform.reflex.cron.scheduling.api.wire.contract;

import hiconic.platform.reflex.cron.scheduling.api.CronScheduling;
import hiconic.rx.module.api.wire.RxExportContract;

public interface CronSchedulingContract extends RxExportContract {
	CronScheduling scheduling();
}
