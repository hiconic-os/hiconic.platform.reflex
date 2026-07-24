package hiconic.rx.security.web.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.gm.model.security.reason.MissingSession;
import com.braintribe.gm.model.security.reason.SecurityReason;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.reflection.ConfigurableCloningContext;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.securityservice.OpenUserSession;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.securityservice.web.GetWebAuthorization;
import com.braintribe.model.securityservice.web.UserPassWebAuthenticate;
import com.braintribe.model.securityservice.web.WebAuthorization;
import com.braintribe.model.securityservice.web.WebAuthorizationRequest;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.lcd.StringTools;

import dev.hiconic.servlet.api.HttpServletArguments;
import dev.hiconic.servlet.api.HttpServletArgumentsAttribute;
import hiconic.rx.security.web.api.CookieHandler;

/**
 * Cookie-oriented web authorization services.
 * <p>
 * The response deliberately contains no session id. Authentication state stays in the HTTP-only cookie while clients can still
 * reflect the current user and roles through {@link GetWebAuthorization}.
 */
public class WebAuthorizationServiceProcessor
		extends AbstractDispatchingServiceProcessor<WebAuthorizationRequest, WebAuthorization> {

	private static final Logger logger = Logger.getLogger(WebAuthorizationServiceProcessor.class);

	private final CookieHandler cookieHandler;

	public WebAuthorizationServiceProcessor(CookieHandler cookieHandler) {
		this.cookieHandler = cookieHandler;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<WebAuthorizationRequest, WebAuthorization> dispatching) {
		dispatching.registerReasoned(UserPassWebAuthenticate.T, this::authenticate);
		dispatching.registerReasoned(GetWebAuthorization.T, this::currentAuthorization);
	}

	private Maybe<? extends WebAuthorization> authenticate(ServiceRequestContext context, UserPassWebAuthenticate request) {
		HttpServletArguments servlet = context.findOrNull(HttpServletArgumentsAttribute.class);
		if (servlet == null)
			return Reasons.build(UnsupportedOperation.T).text("Web authentication requires an HTTP endpoint.").toMaybe();

		Reason validationError = validate(request);
		if (validationError != null)
			return validationError.asMaybe();

		OpenUserSession open = OpenUserSession.T.create();
		open.setLocale(request.getLocale());
		open.setMetaData(request.getMetaData());
		open.setCredentials(UserPasswordCredentials.forUserName(request.getUser(), request.getPassword()));

		Maybe<? extends OpenUserSessionResponse> response = open.eval(context).getReasoned();
		if (response.isUnsatisfied()) {
			if (response.isUnsatisfiedBy(SecurityReason.T))
				return response.propagateReason();

			return InternalError
					.createTraceback(response.whyUnsatisfied(), "Error while authenticating the web request", logger::error)
					.asMaybe();
		}

		UserSession session = response.get().getUserSession();
		cookieHandler.ensureCookie(servlet.getRequest(), servlet.getResponse(), session.getSessionId(), request.getStaySignedIn());
		return Maybe.complete(toAuthorization(session));
	}

	private Maybe<? extends WebAuthorization> currentAuthorization(ServiceRequestContext context,
			@SuppressWarnings("unused") GetWebAuthorization request) {
		UserSession session = context.findOrNull(UserSessionAspect.class);
		if (session == null)
			return Reasons.build(MissingSession.T).text("No authenticated web session found.").toMaybe();

		return Maybe.complete(toAuthorization(session));
	}

	private static Reason validate(UserPassWebAuthenticate request) {
		if (StringTools.isBlank(request.getUser()))
			return Reasons.build(InvalidArgument.T).text("Property user must not be empty.").toReason();
		if (request.getPassword() == null)
			return Reasons.build(InvalidArgument.T).text("Property password must not be null.").toReason();
		return null;
	}

	private static WebAuthorization toAuthorization(UserSession session) {
		WebAuthorization authorization = WebAuthorization.T.create();
		authorization.setEffectiveRoles(session.getEffectiveRoles());

		User user = session.getUser();
		User safeUser = user.clone(ConfigurableCloningContext.build().done());
		safeUser.setPassword(null);
		authorization.setUser(safeUser);
		return authorization;
	}
}
