package hiconic.rx.webapi.client.api;

import java.util.Set;

import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.webapi.client.model.configuration.GmHttpClient;

/**
 * @author peter.gazdik
 */
public interface ClientsFactory {

	HttpClient createGmHttpClient(GmHttpClient configuration);

	/**
	 * Registers a ServiceProcessor which delegates a request via an {@link HttpClient}, configured via HttpPr
	 */
	ServiceProcessor<ServiceRequest, Object> createMdBasedWebApiClientProcessor(Set<String> mdUseCases);

}
