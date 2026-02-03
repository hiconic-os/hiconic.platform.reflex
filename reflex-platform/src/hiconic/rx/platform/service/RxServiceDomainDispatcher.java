// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.service;

import java.util.List;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.DomainIdAspect;
import com.braintribe.model.service.api.DomainRequest;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.model.service.processing.md.ProcessWith;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomains;

public class RxServiceDomainDispatcher
		implements ReasonedServiceProcessor<ServiceRequest, Object>, ReasonedServiceAroundProcessor<ServiceRequest, Object> {

	private static final Logger log = Logger.getLogger(RxServiceDomainDispatcher.class);

	private ServiceDomains serviceDomains;

	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}

	// ###################################################
	// ## . . . . . . . Service Processor . . . . . . . ##
	// ###################################################

	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, ServiceRequest request) {
		String domainId = context.getDomainId();

		ServiceDomain serviceDomain = serviceDomains.byId(domainId);

		return serviceDomain.evaluator().eval(request).getReasoned();
	}

	// ###################################################
	// ## . . . . . Service Around Processor . . . . . .##
	// ###################################################

	/**
	 * Configures {@link DomainIdAspect} for the {@link ServiceRequestContext}, so that #processReasoned(ServiceRequestContext, ServiceRequest) can
	 * delegate to the evaluator of the right service domain.
	 */
	@Override
	public Maybe<? extends Object> processReasoned(ServiceRequestContext context, ServiceRequest request, ProceedContext proceedContext) {
		String domainId = resolveDomainId(request);

		EntityType<? extends ServiceRequest> requestType = request.entityType();

		if (domainId == null) {
			List<? extends ServiceDomain> dependers = serviceDomains.listDomains(requestType);

			if (dependers.size() != 1)
				dependers.removeIf(domain -> !domainProcessesRequest(domain, requestType));

			if (dependers.size() != 1)
				return wrongNumberOfDependers(dependers, requestType);

			domainId = dependers.get(0).domainId();

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

	private String resolveDomainId(ServiceRequest serviceRequest) {
		if (serviceRequest instanceof DomainRequest dr)
			return dr.getDomainId();

		return null;
	}

	private boolean domainProcessesRequest(ServiceDomain domain, EntityType<? extends ServiceRequest> requestType) {
		ProcessWith processWith = domain.contextCmdResolver() //
				.getMetaData() //
				.entityType(requestType) //
				.meta(ProcessWith.T) //
				.exclusive();

		return processWith != null;
	}

	private Maybe<? extends Object> wrongNumberOfDependers(List<? extends ServiceDomain> dependers,
			EntityType<? extends ServiceRequest> requestType) {

		String text = dependers.isEmpty() //
				? "Missing service domain for request " + requestType.getTypeSignature() //
				: "Ambiguous service domains for request " + requestType.getTypeSignature() + ": " + dependers.stream() //
						.map(ServiceDomain::domainId) //
						.collect(Collectors.joining(", ", "[", "]"));

		log.warn(text);

		return Reasons.build(InvalidArgument.T) //
				.text(text) //
				.toMaybe();
	}
}
