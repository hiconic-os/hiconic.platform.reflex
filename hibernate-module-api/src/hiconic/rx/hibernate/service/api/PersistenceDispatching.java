package hiconic.rx.hibernate.service.api;

import com.braintribe.model.service.api.ServiceRequest;

public interface PersistenceDispatching<I extends ServiceRequest, O> {
	void bind(PersistenceDispatchConfiguration<I, O> dispatching);
}
