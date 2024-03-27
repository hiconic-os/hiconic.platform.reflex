// ============================================================================
package hiconic.platform.reflex.security.processor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.model.processing.service.api.ProceedContext;
import com.braintribe.model.processing.service.api.ReasonedServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.SessionIdAspect;
import com.braintribe.model.service.api.AuthorizableRequest;

public class AuthorizingServiceInterceptor implements ReasonedServiceAroundProcessor<AuthorizableRequest, Object> {
	
	private static String getSessionId(ServiceRequestContext requestContext, AuthorizableRequest authorizedRequest) {
		String sessionId = authorizedRequest.getSessionId();
		
		if (sessionId == null) {
			// TODO: find out if this is still used some where
			sessionId = (String)authorizedRequest.getMetaData().get(AuthorizableRequest.sessionId);
			
			if ("".equals(sessionId))
				sessionId = null;
		}
		
		if (sessionId == null) {
			sessionId = requestContext.findAspect(SessionIdAspect.class);
		}

		return sessionId;
	}
	
	@Override
	public Maybe<Object> processReasoned(ServiceRequestContext requestContext, AuthorizableRequest request, ProceedContext proceedContext) {
		Maybe<ServiceRequestContext> contextMaybe = new ContextualizedAuthorization<>(requestContext, requestContext, getSessionId(requestContext, request), requestContext.summaryLogger())
				.withRequest(request)
				.authorizeReasoned(request.requiresAuthentication());
		
		if (contextMaybe.isUnsatisfiedBy(AuthenticationFailure.T))
			return contextMaybe.emptyCast();
		
		return proceedContext.proceedReasoned(contextMaybe.get(), request);
	}
}
