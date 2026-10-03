// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.model.resources.test.wire;

import hiconic.rx.model.resources.test.wire.contract.ModelResourcesTestContract;
import hiconic.rx.model.resources.test.wire.space.ModelResourcesTestModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum ModelResourcesTestModule implements RxModule<ModelResourcesTestModuleSpace> {
	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(ModelResourcesTestContract.class, ModelResourcesTestModuleSpace.class);
	}
}
