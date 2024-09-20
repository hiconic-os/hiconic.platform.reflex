package hiconic.rx.access.module.processing;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.accessapi.CustomPersistenceRequest;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.PersistenceRequest;
import com.braintribe.model.accessapi.QueryAndSelect;
import com.braintribe.model.accessapi.QueryEntities;
import com.braintribe.model.accessapi.QueryProperty;
import com.braintribe.model.accessapi.ReferencesRequest;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.access.module.api.AccessDomain;
import hiconic.rx.access.module.api.AccessDomains;

public class RxPersistenceProcessor implements ReasonedServiceProcessor<PersistenceRequest, Object> {
	private AccessDomains accessDomains;
	
	@Required
	public void setAccessDomains(AccessDomains accessDomains) {
		this.accessDomains = accessDomains;
	}
	
	private Maybe<IncrementalAccess> getDelegate(PersistenceRequest request) {
		AccessDomain accessDomain = accessDomains.byId(request.getServiceId());
		
		if (accessDomain != null)
			return Maybe.complete(accessDomain.incrementalAccess());
		
		return Reasons.build(InvalidArgument.T).text("No such access: " + request.getServiceId()).toMaybe();
	}
	
	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, PersistenceRequest request) {
		Maybe<IncrementalAccess> accessMaybe = getDelegate(request);
		
		if (accessMaybe.isUnsatisfied())
			return accessMaybe;
		
		IncrementalAccess access = accessMaybe.get();

		final Object response;
		
		try {
			switch (request.persistenceRequestType()) {
			case Custom: response = access.processCustomRequest(context, (CustomPersistenceRequest)request); break;
			case GetModel: response = access.getMetaModel(); break;
			case GetPartitions: response = access.getPartitions(); break;
			case ManipulationRequest: response = access.applyManipulation((ManipulationRequest)request); break;
			case QueryAndSelect: response = access.query(((QueryAndSelect)request).getQuery()); break; 
			case QueryEntities: response = access.queryEntities(((QueryEntities)request).getQuery()); break;
			case QueryProperty: response = access.queryProperty(((QueryProperty)request).getQuery()); break;
			case ReferencesRequest: response = access.getReferences((ReferencesRequest)request); break;
			default:
				return Reasons.build(UnsupportedOperation.T) //
						.text("Unsupported request type: " + request.entityType().getTypeSignature()) //
						.toMaybe();
			}
		}
		catch (UnsatisfiedMaybeTunneling t) {
			return t.getMaybe();
		}

		return Maybe.complete(response);
	}
}
