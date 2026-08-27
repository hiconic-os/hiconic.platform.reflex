package hiconic.rx.aop.sp.wire;

import hiconic.rx.aop.sp.api.StateProcessingContract;
import hiconic.rx.aop.sp.wire.space.StateProcessingRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum StateProcessingRxModule implements RxModule<StateProcessingRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(StateProcessingContract.class, StateProcessingRxModuleSpace.class);
	}

}