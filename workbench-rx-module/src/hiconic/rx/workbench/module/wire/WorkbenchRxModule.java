// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.workbench.module.wire;

import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.workbench.api.WorkbenchContract;
import hiconic.rx.workbench.module.wire.space.WorkbenchRxModuleSpace;

public enum WorkbenchRxModule implements RxModule<WorkbenchRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(WorkbenchContract.class, WorkbenchRxModuleSpace.class);
	}

}
