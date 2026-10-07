// ============================================================================
package hiconic.rx.webapi.client.processing;

import java.util.function.Consumer;

import com.braintribe.cfg.Required;
import com.braintribe.exception.HttpException;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.HttpStatusCodeNotification;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.utils.lcd.StopWatch;

import hiconic.rx.webapi.client.api.HttpClient;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpResponse;
import hiconic.rx.webapi.client.api.HttpResponseMapping;
import hiconic.rx.webapi.model.reason.HttpError;
import hiconic.rx.webapi.model.reason.HttpStatusReason;

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
		HttpRequestContext httpContext = null;
		try {

			httpContext = this.httpContextResolver.resolve(context, request);
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
			if (aspect != null && httpContext != null && httpContext.throwExceptionOnErrorCode(e.getStatusCode())) {
				logger.debug(() -> "Notifying HttpStatusCodeNotification aspect about HTTP status code: " + e.getStatusCode());
				aspect.accept(e.getStatusCode());
				return e.getPayload();
			}
			Reason reason = mapFailure(httpContext, e.getStatusCode(), e.getPayload());
			UnsatisfiedMaybeTunneling exc = new UnsatisfiedMaybeTunneling(Maybe.empty(reason));
			throw exc;
		} finally {
			logger.debug(() -> "Finished HTTP execution for ServiceRequest: " + (request != null ? request.entityType().getTypeSignature() : "null")
					+ " after: " + watch.getElapsedTime() + "ms.");
		}
	}

	private Reason mapFailure(HttpRequestContext context, int statusCode, Object payload) {
		HttpResponseMapping mapping = context != null ? context.responseMappingForCode(statusCode) : null;
		HttpStatusReason statusReason = HttpStatusReason.T.create();
		statusReason.setStatusCode(statusCode);
		statusReason.setText("HTTP status " + statusCode);

		if (mapping != null && mapping.kind() == HttpResponseMapping.Kind.BODY && payload instanceof Reason) {
			Reason actualReason = (Reason) payload;
			actualReason.causedBy(statusReason);
			return actualReason;
		}

		Reason detail = null;
		if (mapping != null && mapping.detailType() != null) {
			detail = payload instanceof Reason ? (Reason) payload
					: ParseError.create("HTTP error details could not be decoded as " + mapping.detailType().getTypeSignature());
		}

		Reason main = createReason(mapping != null ? mapping.reasonType() : null);
		String text = detail != null ? detail.getText() : payload != null ? payload.toString() : "Remote service responded with HTTP " + statusCode;
		main.setText(text);
		if (detail != null) {
			detail.causedBy(statusReason);
			main.causedBy(detail);
		} else {
			main.causedBy(statusReason);
		}
		return main;
	}

	@SuppressWarnings("unchecked")
	private Reason createReason(GenericModelType configuredType) {
		if (configuredType instanceof EntityType && Reason.T.isAssignableFrom((EntityType<?>) configuredType))
			return ((EntityType<? extends Reason>) configuredType).create();
		return HttpError.T.create();
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
