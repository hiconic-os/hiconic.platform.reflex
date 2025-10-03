package hiconic.rx.locking.jdbc.wire;

import hiconic.rx.locking.api.LockingContract;
import hiconic.rx.locking.jdbc.wire.space.JdbcLockingRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum JdbcLockingRxModule implements RxModule<JdbcLockingRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(LockingContract.class, JdbcLockingRxModuleSpace.class);
	}

}