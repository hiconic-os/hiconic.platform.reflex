package hiconic.rx.hibernate.service.impl;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.core.expert.impl.PolymorphicDenotationMap;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.annotations.TransactionMode;
import hiconic.rx.hibernate.service.api.PersistenceContext;
import hiconic.rx.hibernate.service.api.PersistenceDispatchConfiguration;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;

public abstract class AbstractDispatchingPersistenceServiceProcessor<I extends ServiceRequest, O> implements PersistenceServiceProcessor<I, O> {
	private Logger log = Logger.getLogger(AbstractDispatchingPersistenceServiceProcessor.class);
	protected DispatchMap dispatchMap = new DispatchMap();
	
	public AbstractDispatchingPersistenceServiceProcessor() {
		configureDispatching(dispatchMap);
	}
	
	protected abstract void configureDispatching(PersistenceDispatchConfiguration<I, O> dispatch);
	
	@Override
	public Maybe<O> process(PersistenceContext context, Session session, I request) {
		EntityType<I> actionType = request.entityType();
		PersistenceProcessorEntry<I, O> entry = dispatchMap.resolve(actionType);
		
		if (entry == null)
			return Reasons.build(UnsupportedOperation.T).text("Unsupported request type:" + request.entityType()).toMaybe();
		
		if (entry.transactionMode == TransactionMode.NONE)
			return (Maybe<O>) entry.processor().process(context, session, request);
		
		FlushMode flushMode = entry.transactionMode == TransactionMode.READ? FlushMode.COMMIT: FlushMode.AUTO;
		
		session.setHibernateFlushMode(flushMode);
		
		Transaction transaction = null;
		try {
			log.trace(() -> "Beginning transaction for operation: " + request.entityType().getTypeSignature());
			transaction = session.beginTransaction();

			Maybe<O> resultMaybe = (Maybe<O>) entry.processor().process(context, session, request); 
			
			// erroneous result
			if (resultMaybe.isUnsatisfied()) {
				try {
					transaction.rollback();
				} catch (RuntimeException rollbackException) {
					log.error("Error while rollbacking transaction after Reason was returned for operation: "+request.entityType().getTypeSignature(), rollbackException);
				}
				return resultMaybe.cast();
			}
			
			// successful result
			if (entry.transactionMode() == TransactionMode.READ) {
				Transaction t = transaction;
				transaction = null;
				t.rollback();
				log.trace(() -> "Successfully rolled-back read-only transaction for operation: " + request.entityType().getTypeSignature());

			} else {
				transaction.commit();
				log.trace(() -> "Successfully committed transaction for operation: " + request.entityType().getTypeSignature());
			}

			return resultMaybe;

		} catch (RuntimeException e) {
			rollbackTransaction(actionType, transaction, e);
			throw e;
		}
	}
	
	private void rollbackTransaction(EntityType<? extends I> actionType, Transaction transaction, RuntimeException e) {
		log.trace(() -> "Transaction failed for operation: " + actionType.getTypeSignature() + ". Error:" + e.getMessage());

		if (transaction != null)
			doRollbackSafe(transaction, e);
	}

	private void doRollbackSafe(Transaction transaction, RuntimeException e) {
		try {
			transaction.rollback();
		} catch (RuntimeException rollbackException) {
			e.addSuppressed(rollbackException);
		}
	}

	public record PersistenceProcessorEntry<I extends ServiceRequest, O>(PersistenceServiceProcessor<I, ? extends O> processor, TransactionMode transactionMode) {}
	
	protected class DispatchMap implements PersistenceDispatchConfiguration<I, O> {
		private PolymorphicDenotationMap<I, PersistenceProcessorEntry<? extends I, O>> mappings = new PolymorphicDenotationMap<>();
		
		@Override
		public <R extends I, AO extends O> void register(EntityType<R> requestType, TransactionMode transactionMode, PersistenceServiceProcessor<R, AO> processor) {
			PersistenceProcessorEntry<R, O> entry = new PersistenceProcessorEntry<>(processor, transactionMode);
			mappings.put(requestType, entry);
		}
		
		public PersistenceProcessorEntry<I, O> resolve(EntityType<? extends I> requestType) {
			PersistenceProcessorEntry<I, O> persistenceProcessorEntry = mappings.find(requestType);
			return persistenceProcessorEntry;
		}
	}
}


