// ============================================================================
package hiconic.rx.webapi.client.processing;

import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.webapi.client.api.HttpRequestContext;

public interface HttpContextResolver {

	HttpRequestContext resolve(ServiceRequestContext serviceContext, ServiceRequest serviceRequest); 
	
}
