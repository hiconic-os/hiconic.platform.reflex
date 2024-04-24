package hiconic.rx.access.module.wire;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessExpertContract;
import hiconic.rx.access.module.wire.space.AccessRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum AccessRxModule implements RxModule<AccessRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(AccessContract.class, AccessRxModuleSpace.class);
		exports.bind(AccessExpertContract.class, AccessRxModuleSpace.class);
	}
}