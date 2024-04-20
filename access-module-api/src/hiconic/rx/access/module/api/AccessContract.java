package hiconic.rx.access.module.api;

import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.wire.RxExportContract;

public interface AccessContract extends RxExportContract {

	void deploy(Access access);
	
	PersistenceGmSessionFactory contextSessionFactory();
	
	PersistenceGmSessionFactory sessionFactory();
	
}
