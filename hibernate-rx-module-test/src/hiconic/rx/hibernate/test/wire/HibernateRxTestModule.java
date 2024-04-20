package hiconic.rx.hibernate.test.wire;

import hiconic.rx.hibernate.test.wire.contract.HibernateTestContract;
import hiconic.rx.hibernate.test.wire.space.HibernateRxTestModuleSpace;
import hiconic.rx.module.api.wire.Exports;
import hiconic.rx.module.api.wire.RxModule;

public enum HibernateRxTestModule implements RxModule<HibernateRxTestModuleSpace> {
	INSTANCE;
	
	@Override
	public void bindExports(Exports exports) {
		exports.bind(HibernateTestContract.class, HibernateRxTestModuleSpace.class);
	}
}