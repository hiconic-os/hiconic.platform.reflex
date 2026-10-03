// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.model.resources.wire;

import hiconic.rx.model.resources.api.ModelResourcesContract;
import hiconic.rx.model.resources.wire.space.ModelResourcesRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum ModelResourcesRxModule implements RxModule<ModelResourcesRxModuleSpace> {
	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(ModelResourcesContract.class, ModelResourcesRxModuleSpace.class);
	}
}
