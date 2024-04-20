package hiconic.rx.hibernate.service.api;

import org.hibernate.SessionFactory;

import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public interface HibernatePersistence {
	
	SessionFactory sessionFactory();
	
	<P extends ServiceRequest, R> ServiceProcessor<P, R> asServiceProcessor(PersistenceDispatching<P, R> dispatching);
	<P extends ServiceRequest, R> ServiceProcessor<P, R> asServiceProcessor(PersistenceProcessor<P, R> processor);

}
