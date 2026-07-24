package hiconic.rx.webapi.client.api;

import java.util.Set;

import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.webapi.client.model.configuration.WebApiRemoteProcessor;

/**
 * @author peter.gazdik
 */
public interface ClientsFactory {

	HttpClient createHttpClient(WebApiRemoteProcessor configuration);

	/**
	 * Creates a {@link ServiceProcessor} which delegates requests via the given {@link HttpClient}.
	 */
	ServiceProcessor<ServiceRequest, Object> createMdBasedWebApiClientProcessor(HttpClient client, Set<String> mdUseCases);

}
