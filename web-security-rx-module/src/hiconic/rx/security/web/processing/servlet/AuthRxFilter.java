// ============================================================================
package hiconic.rx.security.web.processing.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.InitializationAware;
import com.braintribe.cfg.Required;
import com.braintribe.codec.Codec;
import com.braintribe.codec.string.MapCodec;
import com.braintribe.codec.string.UrlEscapeCodec;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.AttributeContextBuilder;
import com.braintribe.exception.HttpException;
import com.braintribe.exception.LogPreferences;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.gm.model.security.reason.MissingCredentials;
import com.braintribe.gm.model.security.reason.SecurityReason;
import com.braintribe.logging.Logger;
import com.braintribe.logging.Logger.LogLevel;
import com.braintribe.logging.ThreadRenamer;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.securityservice.api.attributes.LenientAuthenticationFailure;
import com.braintribe.model.processing.securityservice.api.attributes.OpenUserSessionEntryPointAttribute;
import com.braintribe.model.processing.service.api.aspect.IsAuthorizedAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorSessionIdAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorUserNameAspect;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;
import com.braintribe.model.securityservice.OpenUserSession;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Unsatisfied;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.utils.lcd.LazyInitialized;

import dev.hiconic.servlet.api.HttpFilter;
import dev.hiconic.servlet.impl.util.ServletTools;
import hiconic.rx.security.web.processing.WebSecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AuthRxFilter handles authentication of the request if needed. It:
 * <ul>
 * <li>extracts {@link Credentials} (sessionId/token/user+password) with help of configured {@link WebCredentialsProvider web credential providers}
 * from the request
 * <li>
 * <li>opens a {@link UserSession} with {@link OpenUserSession}
 * <li>optionally checks authorization for this request - if {@link #setGrantedRoles(Set) granted roles} are configured, it verifies at least one of
 * them is present in the UserSession's {@link UserSession#getEffectiveRoles() effective roles}
 * </ul>
 * 
 * All relevant information of an authorized {@link UserSession} will be pushed as a new {@link AttributeContext} and the filter will proceed. The set
 * aspects are:
 * <ul>
 * <li>{@link UserSessionAspect}
 * <li>{@link IsAuthorizedAspect}
 * <li>{@link RequestorUserNameAspect}
 * <li>{@link RequestorSessionIdAspect}
 * </ul>
 * 
 * <p>
 * In case of an authentication or authorization problem the filter will act differently based its {@link #setStrict(boolean) strictness}. If strict
 * it will not proceed and will directly respond according to configuration. If lenient it will proceed without a {@link UserSession} but preserve the
 * reasoning of the missing {@link UserSession} with a {@link LenientAuthenticationFailure} attribute pushed as a new {@link AttributeContext}.
 * 
 * @author dirk.scheffler
 * @author roman.kurmanowytsch
 *
 */
public class AuthRxFilter implements HttpFilter, InitializationAware {

	private final Logger log = Logger.getLogger(AuthRxFilter.class);

	private String relativeLoginPath = "/login";
	private Codec<Map<String, String>, String> urlParamCodec;
	private boolean strict = true;
	private Set<String> grantedRoles = Collections.emptySet();
	private Evaluator<ServiceRequest> requestEvaluator;
	private ThreadRenamer threadRenamer = ThreadRenamer.NO_OP;
	private final Map<String, WebCredentialsProvider> webCredentialProviders = new LinkedHashMap<>();

	private boolean throwExceptionOnAuthFailure = false;
	
	private Function<HttpServletRequest, OpenUserSessionEntryPoint> entryPointProvider = r -> null;

	@Configurable
	public void setEntryPointProvider(Function<HttpServletRequest, OpenUserSessionEntryPoint> entryPointProvider) {
		this.entryPointProvider = entryPointProvider;
	}

	@Configurable
	public void addWebCredentialProvider(String key, WebCredentialsProvider webSessionProvider) {
		synchronized (webCredentialProviders) {
			webCredentialProviders.put(key, webSessionProvider);
		}
	}

	@Configurable
	public void setThrowExceptionOnAuthFailure(boolean throwExceptionOnAuthFailure) {
		this.throwExceptionOnAuthFailure = throwExceptionOnAuthFailure;
	}

	@Configurable
	public void setGrantedRoles(Set<String> grantedRoles) {
		this.grantedRoles = Objects.requireNonNull(grantedRoles, "grantedRoles must not be null");
	}

	@Configurable
	public void setStrict(boolean strict) {
		this.strict = strict;
	}

	@Configurable
	public void setRelativeLoginPath(String relativeLoginPath) {
		this.relativeLoginPath = relativeLoginPath;
	}

	public void setUrlParamCodec(Codec<Map<String, String>, String> urlParamCodec) {
		this.urlParamCodec = urlParamCodec;
	}

	public Codec<Map<String, String>, String> getUrlParamCodec() {
		if (urlParamCodec == null) {
			MapCodec<String, String> mapCodec = new MapCodec<>();
			mapCodec.setEscapeCodec(new UrlEscapeCodec());
			mapCodec.setDelimiter("&");
			this.urlParamCodec = mapCodec;
		}
		return urlParamCodec;
	}

	@Override
	public void postConstruct() {
		if (!strict && !grantedRoles.isEmpty())
			throw new IllegalStateException("If grantedRoles is not empty strict must be true");
	}

	private class StatefulAuthFilter {
		private final HttpServletRequest request;
		private final HttpServletResponse response;
		private final FilterChain chain;
		
		private AuthenticationFailure lenientAuthenticationFailure;

		public StatefulAuthFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
			super();
			this.request = request;
			this.response = response;
			this.chain = chain;
		}

		/**
		 * @param userSession
		 *            the {@link UserSession} that was authenticated and authorized or null if there was none
		 */
		private void proceed(UserSession userSession) throws IOException, ServletException {
			final AttributeContextBuilder contextBuilder = AttributeContexts.peek().derive();
			
			if (lenientAuthenticationFailure != null)
				contextBuilder.set(LenientAuthenticationFailure.class, lenientAuthenticationFailure);

			if (userSession != null) {
				if (log.isTraceEnabled())
					log.trace("Found valid session: " + userSession);

				contextBuilder //
						.set(RequestorSessionIdAspect.class, userSession.getSessionId()) //
						.set(RequestorUserNameAspect.class, userSession.getUser().getName()) //
						.set(IsAuthorizedAspect.class, true) //
						.set(UserSessionAspect.class, userSession);
			}

			threadRenamer.push(() -> "as(" + threadNamePart(userSession) + ")");
			AttributeContexts.push(contextBuilder.build());

			try {
				chain.doFilter(request, response);
			} finally {
				threadRenamer.pop();
				AttributeContexts.pop();
			}
		}

		public void doFilter() throws IOException, ServletException {
			OpenUserSessionEntryPoint entryPoint = entryPointProvider.apply(request);
			
			if (entryPoint != null) {
				AttributeContexts.push(AttributeContexts.derivePeek().set(OpenUserSessionEntryPointAttribute.class, entryPoint).build());
				try {
					doFilter_();
				}
				finally {
					AttributeContexts.pop();
				}
			}
			else
				doFilter_();
		}

		private void doFilter_() throws IOException, ServletException {
			Maybe<UserSession> sessionMaybe = authorize();

			if (sessionMaybe.isUnsatisfied()) {
				respondOnAuthenticationFailure(sessionMaybe.whyUnsatisfied());
			} else {
				proceed(sessionMaybe.get());
			}
		}

		private void respondOnAuthenticationFailure(Reason whyUnsatisfied) throws IOException {
			Reason authFailure = maskWithAuthenticationFailureIfNecessary(whyUnsatisfied);

			String message = authFailure.stringify();

			if (shouldSendRedirectOnAuthFailure(request)) {
				sendLoginRedirect(message);
			} else if (throwExceptionOnAuthFailure) {
				throwHttpException(authFailure, message);
			} else {
				sendUnauthorized(message);
			}
		}

		private void sendUnauthorized(String message) throws IOException {
			log.debug(message);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().print(message);
		}

		private void throwHttpException(Reason authFailure, String message) {
			int statusCode = authFailure instanceof Forbidden ? HttpServletResponse.SC_FORBIDDEN : HttpServletResponse.SC_UNAUTHORIZED;

			HttpException httpException = new HttpException(statusCode, message);
			httpException.setLogPreferences(new LogPreferences(LogLevel.INFO, false, LogLevel.TRACE));
			httpException.withPayload(Unsatisfied.from(Maybe.empty(authFailure)));

			throw httpException;
		}

		private void sendLoginRedirect(String message) throws IOException {
			log.debug(message);

			String contextUrl = getContextUrl(request);

			String continueUrl = contextUrl + request.getServletPath();

			if (request.getPathInfo() != null)
				continueUrl += request.getPathInfo();

			if (request.getQueryString() != null) {
				continueUrl += "?" + request.getQueryString();
			}

			response.sendRedirect(buildLoginPath(request, message, continueUrl));
		}

		private AuthenticationFailure wrapWithAuthenticationFailureIfNecessary(Reason whyUnsatisfied) {
			if (whyUnsatisfied instanceof AuthenticationFailure)
				return (AuthenticationFailure) whyUnsatisfied;

			return Reasons.build(AuthenticationFailure.T).text("Authentication failed.").cause(whyUnsatisfied).toReason();
		}

		private Reason maskWithAuthenticationFailureIfNecessary(Reason whyUnsatisfied) {
			if (whyUnsatisfied instanceof SecurityReason)
				return whyUnsatisfied;

			return Reasons.build(AuthenticationFailure.T).text("Authentication failed.").toReason();
		}

		private String getContextUrl(HttpServletRequest request) {
			return ServletTools.getServletContextUrlProxyAware(request);
		}

		private boolean shouldSendRedirectOnAuthFailure(HttpServletRequest request) {
			return relativeLoginPath != null && ServletTools.getAcceptedMimeTypes(request).contains("text/html");
		}

		/* private Maybe<UserSession> checkAccessGranted(UserSession session) { if (session != null) { boolean grantedByRoles =
		 * (CollectionTools.isEmpty(grantedRoles) || CollectionTools.containsAny(grantedRoles, session.getEffectiveRoles()));
		 * 
		 * if (grantedByRoles) return Maybe.complete(session);
		 * 
		 * if (authorizationFreePredicate.test(request)) { return Maybe.complete(session); }
		 * 
		 * } else { if (authorizationFreePredicate.test(request)) { return Maybe.complete(session); } }
		 * 
		 * return Reasons.build(Forbidden.T).text("Insufficient priviledges to access endpoint").toMaybe(session); } */

		private Maybe<UserSession> checkAccessGranted(Maybe<UserSession> sessionMaybe) {
			if (sessionMaybe.isUnsatisfied()) {
				if (strict)
					return sessionMaybe;

				Reason whyUnsatisfied = sessionMaybe.whyUnsatisfied();

				/* An AuthenticationFailure suggests that there were credentials that could not be authenticated which is maybe relevant to the nested
				 * processing. Other reasons suggest internal problems which are not meaningful for the nested level. */
				if (!sessionMaybe.isUnsatisfiedBy(MissingCredentials.T)) {
					log.info("Lenient authentication failure: " + whyUnsatisfied.stringify());
				}

				lenientAuthenticationFailure = wrapWithAuthenticationFailureIfNecessary(whyUnsatisfied);
				
				return Maybe.complete(null);
			}

			UserSession session = sessionMaybe.get();

			boolean authorized = (CollectionTools.isEmpty(grantedRoles) || CollectionTools.containsAny(grantedRoles, session.getEffectiveRoles()));

			if (authorized)
				return Maybe.complete(session);

			return Reasons.build(Forbidden.T).text("Insufficient priviledges to access endpoint").toMaybe(session);
		}

		private String buildLoginPath(HttpServletRequest request, String message, String continuePath) {
			String servicesPath = getContextUrl(request);

			Map<String, String> params = new HashMap<>();
			if (message != null) {
				params.put(WebSecurityConstants.REQUEST_PARAM_MESSAGE, message);
			}
			if (continuePath != null) {
				params.put(WebSecurityConstants.REQUEST_PARAM_CONTINUE, continuePath);
			}

			String path = servicesPath + relativeLoginPath;
			if (params.size() > 0)
				path += "?" + getUrlParamCodec().encode(params);

			return path;

		}

		private Maybe<Credentials> findCredentials(HttpServletRequest request) {
			LazyInitialized<Reason> invalidCredentialProblems = new LazyInitialized<>(
					() -> Reasons.build(InvalidCredentials.T).text("Error while extracting credentials from http request").toReason());

			for (Map.Entry<String, WebCredentialsProvider> entry : webCredentialProviders.entrySet()) {
				WebCredentialsProvider webCredentialProvider = entry.getValue();

				Maybe<Credentials> credentialsMaybe = webCredentialProvider.provideCredentials(request);

				if (credentialsMaybe.isUnsatisfied()) {
					if (credentialsMaybe.isUnsatisfiedBy(MissingCredentials.T) || credentialsMaybe.isUnsatisfiedBy(NotFound.T))
						continue;

					if (credentialsMaybe.isUnsatisfiedBy(InvalidCredentials.T)) {
						invalidCredentialProblems.get().getReasons().add(credentialsMaybe.whyUnsatisfied());
						continue;
					}

					log.error(credentialsMaybe.whyUnsatisfied().stringify());
				} else {
					return credentialsMaybe;
				}
			}

			if (invalidCredentialProblems.isInitialized()) {
				// TODO: wrap or don't wrap if there is only one invalid credentials
				return invalidCredentialProblems.get().asMaybe();
			}

			return Reasons.build(MissingCredentials.T).text("No credentials found in http request").toMaybe();
		}

		/**
		 * Does an authorization if possible depending on the configuration
		 * 
		 * @return A {@link Maybe} of a {@link UserSession} which also can be null if there was no authorization achieved and also not required.
		 */
		private Maybe<UserSession> authorize() {
			return checkAccessGranted(openUserSession());
		}

		private Maybe<UserSession> openUserSession() {
			Maybe<Credentials> credentialsMaybe = findCredentials(request);

			if (credentialsMaybe.isUnsatisfied())
				return Maybe.empty(credentialsMaybe.whyUnsatisfied());

			Credentials credentials = credentialsMaybe.get();

			return openUserSession(credentials);
		}

		private Maybe<UserSession> openUserSession(Credentials credentials) {
			try {
				OpenUserSession vus = OpenUserSession.T.create();
				vus.setCredentials(credentials);

				EvalContext<? extends OpenUserSessionResponse> vusResponseContext = vus.eval(requestEvaluator);
				Maybe<? extends OpenUserSessionResponse> openUserSessionMaybe = vusResponseContext.getReasoned();

				if (openUserSessionMaybe.isUnsatisfied()) {
					return openUserSessionMaybe.propagateReason();
				}

				return Maybe.complete(openUserSessionMaybe.get().getUserSession());

			} catch (Exception e) {
				String uuid = UUID.randomUUID().toString();
				String msg = "Exception while opening usersession with credentials (context=" + uuid + ").";
				log.error(msg + " [" + credentials + "]", e);
				return InternalError.from(e, msg).asMaybe();
			}
		}
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
		new StatefulAuthFilter(request, response, chain).doFilter();
	}

	/**
	 * @param userSession
	 *            the UserSession from which the thread name will be build or null to see a name with unauthorized mark
	 */
	private String threadNamePart(UserSession userSession) {

		if (userSession != null) {
			try {
				String sessionId = userSession.getSessionId();
				int l = sessionId.length();
				if (l > 5) {
					String s = sessionId.substring(0, 2).concat("-").concat(sessionId.substring(l - 2, l));
					sessionId = s;
				}
				return userSession.getUser().getName() + ":" + sessionId;
			} catch (Exception e) {
				log.debug(() -> "Could not get user name from session", e);
				return "anonymous";
			}
		}

		return "unauthorized";
	}

	@Required
	@Configurable
	public void setRequestEvaluator(Evaluator<ServiceRequest> requestEvaluator) {
		this.requestEvaluator = requestEvaluator;
	}

	@Configurable
	public void setThreadRenamer(ThreadRenamer threadRenamer) {
		Objects.requireNonNull(threadRenamer, "threadRenamer cannot be set to null");
		this.threadRenamer = threadRenamer;
	}
}
