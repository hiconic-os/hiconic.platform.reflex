// ============================================================================
package hiconic.rx.webapi.client.processing;

import java.util.function.Consumer;

import com.braintribe.cfg.Required;
import com.braintribe.exception.HttpException;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.HttpStatusCodeNotification;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.lcd.StopWatch;

import hiconic.rx.webapi.client.api.HttpClient;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpResponse;
import hiconic.rx.webapi.model.reason.HttpReason;

public class WebApiClientServiceProcessor implements ServiceProcessor<ServiceRequest, Object> {

	private static final Logger logger = Logger.getLogger(WebApiClientServiceProcessor.class);

	private HttpContextResolver httpContextResolver;

	@Required
	public void setHttpContextResolver(HttpContextResolver httpContextResolver) {
		this.httpContextResolver = httpContextResolver;
	}

	// ***************************************************************************************************
	// ServiceProcessor
	// ***************************************************************************************************

	@Override
	public Object process(ServiceRequestContext context, ServiceRequest request) {
		StopWatch watch = stopWatch();
		try {

			HttpRequestContext httpContext = this.httpContextResolver.resolve(context, request);
			logger.trace(() -> "Context creation for HTTP execution of ServiceRequest: " + request + " took: " + watch.getElapsedTime() + "ms.");

			HttpClient httpClient = httpContext.httpClient();
			HttpResponse response = httpClient.sendRequest(httpContext);
			logger.trace(() -> "Sending the http request for: " + request + " took: " + watch.getElapsedTime() + "ms.");

			return response.combinedResponse();

		} catch (HttpException e) {
			logger.debug(() -> "HTTP execution for ServiceRequest: " + (request != null ? request.entityType().getTypeSignature() : "null")
					+ " failed with status code: " + e.getStatusCode() + " and payload " + e.getPayload() + " after: " + watch.getElapsedTime()
					+ " ms.");

			Consumer<Integer> aspect = context.findAspect(HttpStatusCodeNotification.class);
			if (aspect != null) {
				logger.debug(() -> "Notifying HttpStatusCodeNotification aspect about HTTP status code: " + e.getStatusCode());
				aspect.accept(e.getStatusCode());
				return e.getPayload();
			}
			HttpReason reason = HttpReason.T.create();
			reason.setHttpCode(e.getStatusCode());
			reason.setHttpPayload(e.getPayload() != null ? e.getPayload().toString() : null);
			UnsatisfiedMaybeTunneling exc = new UnsatisfiedMaybeTunneling(Maybe.empty(reason));
			throw exc;
		} finally {
			logger.debug(() -> "Finished HTTP execution for ServiceRequest: " + (request != null ? request.entityType().getTypeSignature() : "null")
					+ " after: " + watch.getElapsedTime() + "ms.");
		}
	}

	// ***************************************************************************************************
	// Helper
	// ***************************************************************************************************

	private StopWatch stopWatch() {
		StopWatch watch = new StopWatch();
		watch.setAutomaticResetEnabled(true);
		return watch;
	}

}
