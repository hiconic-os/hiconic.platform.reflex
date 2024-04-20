package hiconic.rx.hibernate.service.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.annotations.TransactionMode;

public interface PersistenceDispatchConfiguration<I extends ServiceRequest, O> {
	<R extends I, AO extends O> void register(EntityType<R> requestType, TransactionMode transactionMode, PersistenceServiceProcessor<R, AO> processor);
}