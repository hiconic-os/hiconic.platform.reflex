package hiconic.rx.hibernate.service.api;

import org.hibernate.Session;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.service.api.ServiceRequest;

@FunctionalInterface
public interface PersistenceServiceProcessor<I extends ServiceRequest, O> {
	
	Maybe<O> process(PersistenceContext context, Session session, I request);
}

