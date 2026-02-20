// ============================================================================
package hiconic.rx.platform.resource.jdbc.test.wire;

import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;
import hiconic.rx.platform.resource.jdbc.test.wire.contract.JdbcResourceStorageTestContract;
import hiconic.rx.platform.resource.jdbc.test.wire.space.JdbcResourceStorageTestRxModuleSpace;

public enum JdbcResourceStorageRxTestModule implements RxModule<JdbcResourceStorageTestRxModuleSpace> {
	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(JdbcResourceStorageTestContract.class, JdbcResourceStorageTestRxModuleSpace.class);
	}

}