package hiconic.rx.check.wire;

import hiconic.rx.check.api.CheckContract;
import hiconic.rx.check.wire.space.CheckRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum CheckRxModule implements RxModule<CheckRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(CheckContract.class, CheckRxModuleSpace.class);
	}

}