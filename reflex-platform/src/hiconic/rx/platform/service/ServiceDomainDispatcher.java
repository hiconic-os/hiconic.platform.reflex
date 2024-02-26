package hiconic.rx.platform.service;

import java.util.Objects;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;

public class ServiceDomainDispatcher implements ReasonedServiceProcessor<ServiceRequest, Object> {
	private ServiceDomains serviceDomains;
	
	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}

	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, ServiceRequest request) {
		String domainId = Objects.requireNonNullElse(request.domainId(), "main");
		
		ServiceDomain serviceDomain = serviceDomains.byId(domainId);
		
		if (serviceDomain == null)
			return Reasons.build(InvalidArgument.T).text("Unknown service domain " + domainId).toMaybe();
		
		return serviceDomain.evaluator().eval(request).getReasoned();
	}
}
