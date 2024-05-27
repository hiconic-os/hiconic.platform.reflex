package hiconic.rx.platform.service;

import java.util.List;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.DomainIdAspect;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;

public class RxServiceDomainDispatcher implements ReasonedServiceProcessor<ServiceRequest, Object>, ReasonedServiceAroundProcessor<ServiceRequest, Object> {
	private ServiceDomains serviceDomains;
	
	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}

	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, ServiceRequest request) {
		String domainId = context.getDomainId();
		
		ServiceDomain serviceDomain = serviceDomains.byId(domainId);
		
		return serviceDomain.evaluator().eval(request).getReasoned();
	}
	
	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, ServiceRequest request, ProceedContext proceedContext) {
		String domainId = request.domainId();

		EntityType<? extends ServiceRequest> requestType = request.entityType();
		
		if (domainId == null) {
			List<? extends ServiceDomain> dependers = serviceDomains.listDomains(requestType);
			if (dependers.size() != 1)
				return wrongNumberOfDependers(dependers, requestType);

			domainId =  dependers.get(0).domainId();

		} else {
			ServiceDomain serviceDomain = serviceDomains.byId(domainId);
			if (serviceDomain == null)
				return Reasons.build(InvalidArgument.T) //
						.text("Unknown service domain " + domainId + " on request " + requestType.getTypeSignature()) //
						.toMaybe();

			if (serviceDomain.modelOracle().findEntityTypeOracle(request.entityType()) == null)
				return Reasons.build(UnsupportedOperation.T) //
						.text("Service domain " + domainId + " does not support request " + requestType.getTypeSignature()) //
						.toMaybe();
		}
		
		ServiceRequestContext enrichedContext = context.derive().set(DomainIdAspect.class, domainId).build();
		
		return proceedContext.proceedReasoned(enrichedContext, request);
	}

	private Maybe<? extends Object> wrongNumberOfDependers(List<? extends ServiceDomain> dependers, EntityType<? extends ServiceRequest> requestType) {
		if (dependers.isEmpty())
			return Reasons.build(InvalidArgument.T) //
					.text("Missing service domain for request " + requestType.getTypeSignature()) //
					.toMaybe();
		else
			return Reasons.build(InvalidArgument.T) //
					.text("Ambgious service domains for request " + requestType.getTypeSignature()) //
					.toMaybe();
	}
}
