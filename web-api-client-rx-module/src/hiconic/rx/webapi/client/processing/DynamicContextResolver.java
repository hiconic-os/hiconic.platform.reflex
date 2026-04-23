// ============================================================================
package hiconic.rx.webapi.client.processing;

import java.util.function.Function;

import com.braintribe.cfg.Required;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.webapi.client.api.HttpClient;
import hiconic.rx.webapi.client.model.meta.HttpProcessWith;

public class DynamicContextResolver extends AbstractContextResolver {

	private Function<String, CmdResolver> domainIdToCmdResolver;

	@Required
	public void setDomainIdToCmdResolver(Function<String, CmdResolver> domainIdToCmdResolver) {
		this.domainIdToCmdResolver = domainIdToCmdResolver;
	}

	// ***************************************************************************************************
	// Context Resolver
	// ***************************************************************************************************

	@Override
	protected HttpClient getHttpClient(RequestContextResolver contextResolver) {
		ServiceRequest serviceRequest = contextResolver.serviceRequest;
		HttpProcessWith processWith = contextResolver.modelResolver.entity(serviceRequest).meta(HttpProcessWith.T).exclusive();
		if (processWith == null)
			throw new IllegalArgumentException("No HttpProcessWith configured for request: " + serviceRequest);

		HttpClient httpClient = processWith.getHttpClient();
		if (httpClient == null)
			throw new IllegalArgumentException("No HttpClient configured on the HttpProcessWith configured for request: " + serviceRequest);

		return httpClient;
	}

	@Override
	protected ModelMdResolver getModelResolver(ServiceRequestContext serviceContext, ServiceRequest serviceRequest) {
		String domainId = serviceContext.getDomainId();

		return domainIdToCmdResolver.apply(domainId) //
				.getMetaData() //
				.useCases(resolverUseCases);
	}
}
