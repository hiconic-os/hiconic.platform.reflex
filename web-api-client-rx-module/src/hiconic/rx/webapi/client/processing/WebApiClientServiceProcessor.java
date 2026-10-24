// ============================================================================
package hiconic.rx.webapi.client.processing;

import java.util.function.Consumer;

import com.braintribe.cfg.Required;
import com.braintribe.exception.HttpException;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.CommunicationError;
import com.braintribe.gm.model.reason.essential.ParseError;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.HttpStatusCodeNotification;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.utils.lcd.StopWatch;

import hiconic.rx.webapi.client.api.HttpClient;
import hiconic.rx.webapi.client.api.HttpErrorResponse;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpResponse;
import hiconic.rx.webapi.client.api.HttpResponseDecodingException;
import hiconic.rx.webapi.client.api.HttpResponseMapping;
import hiconic.rx.webapi.model.reason.HttpError;
import hiconic.rx.webapi.model.reason.HttpStatusReason;
import hiconic.rx.webapi.model.reason.HttpStatusWithBody;
import hiconic.rx.webapi.model.reason.HttpStatusWithEntityBody;
import hiconic.rx.webapi.model.reason.HttpStatusWithTextBody;

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

		} catch (HttpResponseDecodingException e) {
			Reason reason = mapDecodingFailure(e.getStatusCode(), e.getResponse());
			throw new UnsatisfiedMaybeTunneling(Maybe.empty(reason));
		} catch (HttpException e) {
			logger.debug(() -> "HTTP execution for ServiceRequest: " + (request != null ? request.entityType().getTypeSignature() : "null")
					+ " failed with status code: " + e.getStatusCode() + " and payload " + e.getPayload() + " after: " + watch.getElapsedTime()
					+ " ms.");

			Consumer<Integer> aspect = context.findAspect(HttpStatusCodeNotification.class);
			if (aspect != null && httpContext != null && httpContext.throwExceptionOnErrorCode(e.getStatusCode())) {
				logger.debug(() -> "Notifying HttpStatusCodeNotification aspect about HTTP status code: " + e.getStatusCode());
				aspect.accept(e.getStatusCode());
				return decodedPayload(e.getPayload());
			}
			Reason reason = mapFailure(httpContext, e.getStatusCode(), e.getPayload());
			UnsatisfiedMaybeTunneling exc = new UnsatisfiedMaybeTunneling(Maybe.empty(reason));
			throw exc;
		} finally {
			logger.debug(() -> "Finished HTTP execution for ServiceRequest: " + (request != null ? request.entityType().getTypeSignature() : "null")
					+ " after: " + watch.getElapsedTime() + "ms.");
		}
	}

	static Reason mapFailure(HttpRequestContext context, int statusCode, Object payload) {
		HttpResponseMapping mapping = context != null ? context.responseMappingForCode(statusCode) : null;
		HttpErrorResponse errorResponse = payload instanceof HttpErrorResponse ? (HttpErrorResponse) payload : null;
		Object decodedBody = errorResponse == null ? payload : errorResponse.decodedBody();

		if (mapping != null && mapping.kind() == HttpResponseMapping.Kind.BODY && decodedBody instanceof Reason) {
			Reason actualReason = (Reason) decodedBody;
			actualReason.causedBy(statusReason(statusCode));
			return actualReason;
		}

		HttpStatusReason statusReason = statusReason(statusCode, errorResponse, decodedBody);
		Reason main = createReason(mapping != null ? mapping.reasonType() : null);
		String text = detailText(decodedBody);
		if (text == null && errorResponse != null) text = errorResponse.textBody();
		if (text == null) text = "Remote service responded with HTTP " + statusCode;
		main.setText(text);
		main.causedBy(statusReason);
		return main;
	}

	static Reason mapDecodingFailure(int statusCode, HttpErrorResponse response) {
		HttpStatusReason status = textStatusReason(statusCode, response);
		ParseError parseError = ParseError.create(response.decodingFailure());
		parseError.causedBy(status);

		CommunicationError reason = CommunicationError.create("Could not decode the remote HTTP response according to its modeled contract");
		reason.causedBy(parseError);
		return reason;
	}

	private static HttpStatusReason textStatusReason(int statusCode, HttpErrorResponse response) {
		HttpStatusWithTextBody reason = HttpStatusWithTextBody.T.create();
		initializeStatus(reason, statusCode, response);
		reason.setBody(response.textBody());
		reason.setExpectedBodyTypeSignature(response.expectedBodyTypeSignature());
		return reason;
	}

	private static HttpStatusReason statusReason(int statusCode, HttpErrorResponse response, Object decodedBody) {
		if (response != null && decodedBody instanceof GenericEntity && response.decodingFailure() == null) {
			HttpStatusWithEntityBody reason = HttpStatusWithEntityBody.T.create();
			initializeStatus(reason, statusCode, response);
			reason.setBody((GenericEntity) decodedBody);
			return reason;
		}
		if (response != null && (response.textBody() != null && !response.textBody().isEmpty() || response.decodingFailure() != null)) {
			HttpStatusWithTextBody reason = HttpStatusWithTextBody.T.create();
			initializeStatus(reason, statusCode, response);
			reason.setBody(response.textBody());
			reason.setExpectedBodyTypeSignature(response.expectedBodyTypeSignature());
			if (response.decodingFailure() != null) reason.causedBy(ParseError.create(response.decodingFailure()));
			return reason;
		}
		return statusReason(statusCode);
	}

	private static HttpStatusReason statusReason(int statusCode) {
		HttpStatusReason reason = HttpStatusReason.T.create();
		reason.setStatusCode(statusCode);
		reason.setText("HTTP status " + statusCode);
		return reason;
	}

	private static void initializeStatus(HttpStatusWithBody reason, int statusCode, HttpErrorResponse response) {
		reason.setStatusCode(statusCode);
		reason.setText("HTTP status " + statusCode);
		reason.setMimeType(response.mimeType());
		reason.setBodySize(response.bodySize());
		reason.setBodyTruncated(response.bodyTruncated());
	}

	private static Object decodedPayload(Object payload) {
		if (!(payload instanceof HttpErrorResponse)) return payload;
		HttpErrorResponse response = (HttpErrorResponse) payload;
		return response.decodedBody() != null ? response.decodedBody() : response.textBody();
	}

	private static String detailText(Object detail) {
		if (detail instanceof Reason) return ((Reason) detail).getText();
		if (!(detail instanceof GenericEntity)) return detail instanceof String ? (String) detail : null;
		GenericEntity entity = (GenericEntity) detail;
		Property property = entity.entityType().findProperty("text");
		if (property == null) property = entity.entityType().findProperty("detail");
		if (property == null) property = entity.entityType().findProperty("title");
		Object value = property == null ? null : property.get(entity);
		return value instanceof String ? (String) value : null;
	}

	@SuppressWarnings("unchecked")
	private static Reason createReason(GenericModelType configuredType) {
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
