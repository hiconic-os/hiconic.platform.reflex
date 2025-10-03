package hiconic.rx.leadership.lockingbased.wire;

import hiconic.rx.leadership.api.LeadershipContract;
import hiconic.rx.leadership.lockingbased.wire.space.LeadershipViaLockingRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum LeadershipViaLockingRxModule implements RxModule<LeadershipViaLockingRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(LeadershipContract.class, LeadershipViaLockingRxModuleSpace.class);
	}

}