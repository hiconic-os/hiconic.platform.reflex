// ============================================================================
package hiconic.platform.reflex.security.processor;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.AttributeContextBuilder;
import com.braintribe.common.attribute.common.UserInfo;
import com.braintribe.common.attribute.common.UserInfoAttribute;
import com.braintribe.exception.AuthorizationException;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.MissingSession;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.securityservice.api.attributes.LenientAuthenticationFailure;
import com.braintribe.model.processing.service.api.ServiceRequestSummaryLogger;
import com.braintribe.model.processing.service.api.aspect.IsAuthorizedAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorSessionIdAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorUserNameAspect;
import com.braintribe.model.processing.service.api.aspect.SummaryLoggerAspect;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.commons.NoOpServiceRequestSummaryLogger;
import com.braintribe.model.securityservice.ValidateUserSession;
import com.braintribe.model.service.api.AuthorizableRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.usersession.UserSession;

public class ContextualizedAuthorization<C extends AttributeContext> {
	private static final String LOGSTEP_AUTH = "Authorization";
	private static final Logger log = Logger.getLogger(ContextualizedAuthorization.class);

	protected C currentContext;
	protected AuthorizableRequest request;
	protected String sessionId;
	protected ServiceRequestSummaryLogger summaryLogger;
	private Evaluator<ServiceRequest> evaluator;

	public ContextualizedAuthorization(Evaluator<ServiceRequest> evaluator, C currentContext, String sessionId) {
		this(evaluator, currentContext, sessionId,
				currentContext.findAttribute(SummaryLoggerAspect.class).orElse(NoOpServiceRequestSummaryLogger.INSTANCE));
	}

	public ContextualizedAuthorization(Evaluator<ServiceRequest> evaluator, C currentContext, String sessionId,
			ServiceRequestSummaryLogger summaryLogger) {
		super();
		this.evaluator = evaluator;
		this.currentContext = currentContext;
		this.sessionId = sessionId;
		this.summaryLogger = summaryLogger;
	}

	public ContextualizedAuthorization<C> withRequest(AuthorizableRequest request) {
		this.request = request;
		return this;
	}

	public Maybe<C> authorizeReasoned(boolean authorizationRequired) {
		String requestSessionId = sessionId;
		UserSession session = currentContext.findAttribute(UserSessionAspect.class).orElse(null);
		
		C effectiveContext = currentContext;

		if (requestSessionId != null) {
			if (session == null || !session.getSessionId().equals(requestSessionId)) {
				Maybe<? extends UserSession> sessionMaybe = validate(requestSessionId);
				
				if (sessionMaybe.isUnsatisfiedBy(AuthenticationFailure.T))
					return sessionMaybe.emptyCast();
				
				session = sessionMaybe.get();
				
				log.trace("Processing with a validated UserSession with a sessionId from AuthorizedRequest");
				effectiveContext = buildAttributeContext(session);
			}

			log.trace("Processing with already existing and matching UserSession from thread");
		} else {
			if (session == null) {
				if (authorizationRequired) {
					AuthenticationFailure lenientAuthenticationFailure = currentContext.findAttribute(LenientAuthenticationFailure.class).orElse(null);
					if (lenientAuthenticationFailure != null) {
						return lenientAuthenticationFailure.asMaybe();
					}
					else
						return Reasons.build(MissingSession.T).text("Request thread is not authorized and request had no sessionId").toMaybe();
				}
				else if (request != null ){
					AuthenticationFailure lenientAuthenticationFailure = currentContext.findAttribute(LenientAuthenticationFailure.class).orElse(null);
					if (lenientAuthenticationFailure != null) {
						return lenientAuthenticationFailure.asMaybe();
					}
				}
			}
		}

		// TODO: think AGAIN! do not mutate requests if avoidable
		if (session != null && request != null) {
			if (request.getSessionId() == null)
				request.setSessionId(session.getSessionId());
		}

		return Maybe.complete(effectiveContext);
	}

	
	public C authorize(boolean authorizationRequired) {
		Maybe<C> effectiveContextMaybe = authorizeReasoned(authorizationRequired);
		
		if (effectiveContextMaybe.isUnsatisfiedBy(AuthenticationFailure.T)) {
			throw new AuthorizationException("Could not successfully authenticate", 
					new ReasonException(effectiveContextMaybe.whyUnsatisfied()));
		}
		
		return effectiveContextMaybe.get();
	}

	private C buildAttributeContext(UserSession userSession) {
		AttributeContextBuilder builder = currentContext.derive();
		builder.set(UserSessionAspect.class, userSession);
		builder.set(IsAuthorizedAspect.class, true);
		builder.set(RequestorSessionIdAspect.class, userSession.getSessionId());
		String userName = userSession.getUser().getName();
		builder.set(RequestorUserNameAspect.class, userName);
		builder.set(UserInfoAttribute.class, UserInfo.of(userName, userSession.getEffectiveRoles()));
		builder.set(LenientAuthenticationFailure.class, null);
		
		return (C) builder.build();
	}

	private Maybe<? extends UserSession> validate(String sessionId) {
		summaryLogger.startTimer(LOGSTEP_AUTH);
		try {
			ValidateUserSession validateUserSession = ValidateUserSession.T.create();
			validateUserSession.setSessionId(sessionId);
			// validateUserSession.getMetaData().put("source", "BasicServiceUserSessionScoping->validateUserSession");
			return validateUserSession.eval(evaluator).getReasoned();
		} finally {
			summaryLogger.stopTimer(LOGSTEP_AUTH);
		}
	}

}