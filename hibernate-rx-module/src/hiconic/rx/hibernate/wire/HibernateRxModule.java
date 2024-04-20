package hiconic.rx.hibernate.wire;

import hiconic.rx.hibernate.module.api.HibernateContract;
import hiconic.rx.hibernate.wire.space.HibernateRxModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum HibernateRxModule implements RxModule<HibernateRxModuleSpace> {

	INSTANCE;

	@Override
	public void bindExports(Exports exports) {
		exports.bind(HibernateContract.class, HibernateRxModuleSpace.class);
	}
}