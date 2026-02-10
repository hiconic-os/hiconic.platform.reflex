package hiconic.platform.reflex.cron.scheduling.wire;

import hiconic.platform.reflex.cron.scheduling.api.wire.contract.CronSchedulingContract;
import hiconic.platform.reflex.cron.scheduling.wire.space.CronSchedulingRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum CronSchedulingRxModule implements RxModule<CronSchedulingRxModuleSpace> {

	INSTANCE;
	
	@Override
	public void bindExports(Exports exports) {
		exports.bind(CronSchedulingContract.class, CronSchedulingRxModuleSpace.class);
	}

}