package hiconic.rx.hibernate.processing;

import java.lang.invoke.MethodHandle;

import org.hibernate.Session;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.service.api.PersistenceContext;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;

public class MethodHandlePersistenceServiceProcessor<P extends ServiceRequest, R> implements PersistenceServiceProcessor<P, R> {
	
	private MethodHandle methodHandle;
	
	public MethodHandlePersistenceServiceProcessor(MethodHandle methodHandle) {
		super();
		this.methodHandle = methodHandle;
	}

	@Override
	public Maybe<R> process(PersistenceContext context, Session session, P request) {
		try {
			return (Maybe<R>) methodHandle.invoke(context, session, request);
		} catch (RuntimeException | Error e) {
			throw e;
		}
		catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

}
