package hiconic.platform.reflex.db.wire;

import hiconic.platform.reflex.db.wire.space.DbRxModuleSpace;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum DbRxModule implements RxModule<DbRxModuleSpace> {
	INSTANCE;
	
	@Override
	public void bindExports(Exports exports) {
		exports.bind(DatabaseContract.class, DbRxModuleSpace.class);
	}
}