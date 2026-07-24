package hiconic.rx.webapi.client.processing;

import java.util.function.Function;

import com.braintribe.cfg.Required;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.builders.ModelMdResolver;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.webapi.client.api.HttpClient;

/** Resolves request mapping metadata dynamically while always using one configured HTTP client. */
public class FixedClientContextResolver extends AbstractContextResolver {

	private HttpClient httpClient;
	private Function<String, CmdResolver> domainIdToCmdResolver;

	@Required
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Required
	public void setDomainIdToCmdResolver(Function<String, CmdResolver> domainIdToCmdResolver) {
		this.domainIdToCmdResolver = domainIdToCmdResolver;
	}

	@Override
	protected HttpClient getHttpClient(RequestContextResolver contextResolver) {
		return httpClient;
	}

	@Override
	protected ModelMdResolver getModelResolver(ServiceRequestContext serviceContext, ServiceRequest serviceRequest) {
		String domainId = serviceContext.getDomainId();
		return domainIdToCmdResolver.apply(domainId).getMetaData().useCases(resolverUseCases);
	}
}
