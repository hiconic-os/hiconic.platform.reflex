package hiconic.rx.platform.service;

import java.util.Objects;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.DomainIdAspect;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;

public class ServiceDomainDispatcher implements ReasonedServiceProcessor<ServiceRequest, Object>, ReasonedServiceAroundProcessor<ServiceRequest, Object> {
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
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, ServiceRequest request,
			ProceedContext proceedContext) {
		String domainId = Objects.requireNonNullElse(request.domainId(), "main");
		
		ServiceDomain serviceDomain = serviceDomains.byId(domainId);
		
		if (serviceDomain == null)
			return Reasons.build(InvalidArgument.T).text("Unknown service domain " + domainId).toMaybe();
		
		if (serviceDomain.modelOracle().findEntityTypeOracle(request.entityType()) == null)
			return Reasons.build(UnsupportedOperation.T).text("Unknown service request type " + request.entityType().getTypeSignature() + " in domain " + domainId).toMaybe();
		
		ServiceRequestContext enrichedContext = context.derive().set(DomainIdAspect.class, domainId).build();
		
		return proceedContext.proceedReasoned(enrichedContext, request);
	}
}
