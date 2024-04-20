package hiconic.rx.hibernate.service.impl;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.annotations.TransactionMode;
import hiconic.rx.hibernate.service.api.PersistenceDispatchConfiguration;
import hiconic.rx.hibernate.service.api.PersistenceDispatching;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;

public class ConfigurableDispatchingPersistenceServiceProcessor<I extends ServiceRequest, O> extends AbstractDispatchingPersistenceServiceProcessor<I, O> implements PersistenceDispatchConfiguration<I, O> {
	
	
	public ConfigurableDispatchingPersistenceServiceProcessor(PersistenceDispatching<I, O> dispatching) {
		dispatching.bind(dispatchMap);
	}
	
	public ConfigurableDispatchingPersistenceServiceProcessor() {
	}
	
	@Override
	protected void configureDispatching(PersistenceDispatchConfiguration<I, O> dispatch) {
		// empty
	}
	
	@Override
	public <R extends I, AO extends O> void register(EntityType<R> requestType, TransactionMode transactionMode,
			PersistenceServiceProcessor<R, AO> processor) {
		dispatchMap.register(requestType, transactionMode, processor);
	}
}


