package hiconic.rx.tools.wire;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.tools.api.ExternalToolsContract;
import hiconic.rx.tools.wire.space.ExternalToolsRxModuleSpace;

public enum ExternalToolsRxModule implements RxModule<ExternalToolsRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(ExternalToolsContract.class, ExternalToolsRxModuleSpace.class);
	}

}
