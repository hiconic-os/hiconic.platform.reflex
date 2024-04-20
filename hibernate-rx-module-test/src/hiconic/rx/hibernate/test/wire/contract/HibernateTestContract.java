package hiconic.rx.hibernate.test.wire.contract;

import org.hibernate.SessionFactory;

import hiconic.rx.module.api.wire.RxExportContract;

public interface HibernateTestContract extends RxExportContract {
	SessionFactory mainSessionFactory();
}
