package hiconic.rx.hibernate.service.api;

import org.hibernate.SessionFactory;

import com.braintribe.model.mpc.ModelPathCondition;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

public interface PersistenceContext {
	SessionFactory sessionFactory();
	ServiceRequestContext requestContext();
	<T> T detach(T value, ModelPathCondition condition);
}
