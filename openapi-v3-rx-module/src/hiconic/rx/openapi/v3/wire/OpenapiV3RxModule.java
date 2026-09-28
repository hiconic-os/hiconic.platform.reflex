package hiconic.rx.openapi.v3.wire;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.openapi.v3.api.OpenapiV3Contract;
import hiconic.rx.openapi.v3.wire.space.OpenapiV3RxModuleSpace;

public enum OpenapiV3RxModule implements RxModule<OpenapiV3RxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(OpenapiV3Contract.class, OpenapiV3RxModuleSpace.class);
	}

}
