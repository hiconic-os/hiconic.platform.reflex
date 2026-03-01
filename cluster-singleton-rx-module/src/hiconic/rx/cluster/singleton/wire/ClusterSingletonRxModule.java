package hiconic.rx.cluster.singleton.wire;

import hiconic.rx.cluster.singleton.wire.space.ClusterSingletonRxModuleSpace;
import hiconic.rx.locking.api.LockingContract;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum ClusterSingletonRxModule implements RxModule<ClusterSingletonRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(LockingContract.class, ClusterSingletonRxModuleSpace.class);
	}
}