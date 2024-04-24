package hiconic.rx.access.module.api;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.module.api.wire.RxExportContract;

public interface AccessContract extends RxExportContract {

	void deploy(Access access);
	
	AccessModelConfigurations accessModelConfigurations();

	PersistenceGmSessionFactory contextSessionFactory();
	
	PersistenceGmSessionFactory systemSessionFactory();
	
	PersistenceGmSessionFactory sessionFactory(AttributeContext attributeContext);
	
}

/*
 * access-configuration-model (defines base denotation type Access)
 * access-module-api
 * access-rx-module (implements: AccessContract & AccessExpertContract)
 *   - expert registration and usage
 *   - reading AccessConfiguration and automatically deploying it
 *   - Access to deployed Accesses via session factories
 *   
 * hibernate-access-configuration-model
 * hibernate-access-rx-module
 * 
 * smood-access-configuration-model
 * smood-access-rx-module
 * 
 */
