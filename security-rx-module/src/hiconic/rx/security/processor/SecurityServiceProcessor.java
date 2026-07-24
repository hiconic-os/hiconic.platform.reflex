// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.security.processor;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.gm.model.security.reason.InvalidSession;
import com.braintribe.gm.model.security.reason.SessionExpired;
import com.braintribe.gm.model.security.reason.SessionNotFound;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.securityservice.api.DeletedSessionInfo;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.processing.securityservice.api.attributes.OpenUserSessionEntryPointAttribute;
import com.braintribe.model.processing.securityservice.impl.Roles;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.security.service.config.OpenUserSessionConfiguration;
import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.AuthenticatedUser;
import com.braintribe.model.securityservice.AuthenticatedUserSession;
import com.braintribe.model.securityservice.GetCurrentUser;
import com.braintribe.model.securityservice.Logout;
import com.braintribe.model.securityservice.LogoutSession;
import com.braintribe.model.securityservice.OpenUserSession;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.SecurityRequest;
import com.braintribe.model.securityservice.ValidateUserSession;
import com.braintribe.model.securityservice.credentials.AbstractUserIdentificationCredentials;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;
import com.braintribe.model.securityservice.credentials.TokenWithUserNameCredentials;
import com.braintribe.model.securityservice.credentials.identification.EmailIdentification;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.securityservice.credentials.identification.UserNameIdentification;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.lcd.Lazy;

import hiconic.rx.security.api.UserService;
import hiconic.rx.security.api.UserSessionAccessVerificationExpert;

public class SecurityServiceProcessor extends AbstractDispatchingServiceProcessor<SecurityRequest, Object> {

	private static final Logger log = Logger.getLogger(SecurityServiceProcessor.class);

	private UserService userService;
	private UserSessionService userSessionService;
	private Evaluator<ServiceRequest> evaluator;
	private List<UserSessionAccessVerificationExpert> userSessionAccessVerificationExperts;

	private boolean enableUserStatistics;
	private TimeSpan sessionMaxIdleTime;
	private TimeSpan sessionMaxAge;
	private String internalRole = "internal";
	private OpenUserSessionConfiguration openUserSessionConfiguration = OpenUserSessionConfiguration.T.create();
	private Lazy<Map<String, OpenUserSessionEntryPoint>> entryPointsByName = new Lazy<>(this::indexEntryPointsByName);
	private final CredentialsHasher credentialsHasher = new CredentialsHasher();
	
	@Required
	@Configurable
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = evaluator;
	}

	@Required
	@Configurable
	public void setUserSessionService(UserSessionService userSessionService) {
		this.userSessionService = userSessionService;
	}
	
	@Required
	@Configurable
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	@Configurable
	public void setOpenUserSessionConfiguration(OpenUserSessionConfiguration openUserSessionConfiguration) {
		this.openUserSessionConfiguration = openUserSessionConfiguration;
		this.entryPointsByName = new Lazy<>(this::indexEntryPointsByName);
	}

	@Configurable
	public void setInternalRole(String internalRole) {
		this.internalRole = internalRole;
	}

	@Configurable
	public void setUserSessionAccessVerificationExperts(List<UserSessionAccessVerificationExpert> userSessionAccessVerificationExperts) {
		this.userSessionAccessVerificationExperts = userSessionAccessVerificationExperts;
	}
	
	@Configurable
	public void setEnableUserStatistics(boolean enableUserStatistics) {
		this.enableUserStatistics = enableUserStatistics;
	}

	/**
	 * <p>
	 * Sets the max idle time, as {@link TimeSpan}, to be set to the {@link UserSession} objects created by this
	 * authentication expert
	 * 
	 * @param sessionMaxIdleTime
	 *            The max idle time to be set to the {@link UserSession} objects created by this authentication expert
	 */
	@Configurable
	public void setSessionMaxIdleTime(TimeSpan sessionMaxIdleTime) {
		this.sessionMaxIdleTime = sessionMaxIdleTime;
	}

	/**
	 * <p>
	 * Sets the max age, as {@link TimeSpan}, to assist the generation of expiry dates for the {@link UserSession} objects
	 * created by this authentication expert
	 * 
	 * @param sessionMaxAge
	 *            Sets the max age to base the expiry dates for the {@link UserSession} objects created by this
	 *            authentication expert
	 */
	@Configurable
	public void setSessionMaxAge(TimeSpan sessionMaxAge) {
		this.sessionMaxAge = sessionMaxAge;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<SecurityRequest, Object> dispatching) {
		dispatching.registerReasoned(OpenUserSession.T, (c, r) -> openUserSession(c, r));
		dispatching.registerReasoned(ValidateUserSession.T, (c, r) -> validateUserSession(c, r));
		dispatching.register(GetCurrentUser.T, (c, r) -> getCurrentUser(c));
		dispatching.register(Logout.T, (c, r) -> logout(c));
		dispatching.register(LogoutSession.T, (c, r) -> logoutSession(r));
	}

	private Map<String, OpenUserSessionEntryPoint> indexEntryPointsByName() {
		Map<String, OpenUserSessionEntryPoint> index = new HashMap<>();
		for (OpenUserSessionEntryPoint entryPoint : openUserSessionConfiguration.getEntryPoints())
			index.put(entryPoint.getName(), entryPoint);
		return index;
	}

	private boolean logoutSession(LogoutSession request) {
		return logout(request.getSessionId());
	}

	private boolean logout(ServiceRequestContext context) {
		return logout(context.getRequestorSessionId());
	}

	private boolean logout(String sessionId) {
		if (sessionId == null)
			return false;

		Maybe<DeletedSessionInfo> deletedUserSessionMaybe = userSessionService.deleteUserSession(sessionId);

		deletedUserSessionMaybe //
				.ifSatisfied(this::collectStatisticsUponLogout) //
				.ifUnsatisfied(r -> log.debug("Could logout session '" + sessionId + "': " + r.stringify()));

		return deletedUserSessionMaybe.isSatisfied();
	}

	private Maybe<OpenUserSessionResponse> openUserSession(ServiceRequestContext requestContext, OpenUserSession openUserSession) {
		Maybe<ValidationResult> validationResultMaybe = validate(requestContext, openUserSession);
		if (validationResultMaybe.isUnsatisfied())
			return validationResultMaybe.propagateReason();

		ValidationResult validationResult = validationResultMaybe.get();
		Credentials credentials = openUserSession.getCredentials();

		String acquirationKey = null;

		if (credentials.acquirationSupportive()) {
			// The idea behind acquiring is that we don't open a new session but use an existing one in some cases
			// E.g. when the client just always sends the same token (3rd party system)
			// But when the requester address is different, i.e. it is the same user on a different device, we want a new session
			acquirationKey = credentialsHasher.hash(credentials, m -> m.put("requestorAddress", requestContext.getRequestorAddress()));

			Maybe<UserSession> acquiredUserSessionMaybe = acquireUserSession(requestContext, acquirationKey);

			if (acquiredUserSessionMaybe.isSatisfied()) {
				UserSession userSession = acquiredUserSessionMaybe.get();
				Reason authorizationFailure = checkAuthorization(validationResult.entryPoint(), userSession.getEffectiveRoles());
				if (authorizationFailure != null)
					return authorizationFailure.asMaybe();

				return Maybe.complete(createResponseFrom(userSession, true));
			}

			// TODO: rethink the responsibility for UserSession transcription and therefore the responsibility of acquiration
			// blocking
			if (acquiredUserSessionMaybe.isUnsatisfiedBy(InvalidCredentials.T)) {
				// In this case the credentials are blocked via the acquiration mechanism and a reauthentication is not possible
				return acquiredUserSessionMaybe.whyUnsatisfied().asMaybe();

			} else if (!acquiredUserSessionMaybe.isUnsatisfiedBy(SessionNotFound.T)) {
				log.debug("Error while finding session via acquiration key: " + acquiredUserSessionMaybe.whyUnsatisfied().stringify());
			}
		}

		AuthenticateCredentials authenticateCredentials = AuthenticateCredentials.T.create();
		authenticateCredentials.setProperties(openUserSession.getProperties());
		authenticateCredentials.setCredentials(openUserSession.getCredentials());

		Maybe<? extends AuthenticateCredentialsResponse> maybe = authenticateCredentials.eval(evaluator).getReasoned();

		if (maybe.isUnsatisfied())
			return Maybe.empty(maybe.whyUnsatisfied());

		AuthenticateCredentialsResponse authenticatedCredentialsResponse = maybe.get();
		if (authenticatedCredentialsResponse instanceof AuthenticatedUserSession authenticatedUserSession) {
			UserSession userSession = authenticatedUserSession.getUserSession();
			Reason authorizationFailure = checkAuthorization(validationResult.entryPoint(), userSession.getEffectiveRoles());
			if (authorizationFailure != null)
				return authorizationFailure.asMaybe();

			return Maybe.complete(createResponseFrom(userSession, true));
		}

		Reason authorizationFailure = checkAuthorization(validationResult.entryPoint(),
				Roles.authenticatedCredentialsEffectiveRoles(authenticatedCredentialsResponse));
		if (authorizationFailure != null)
			return authorizationFailure.asMaybe();

		return buildUserSession(requestContext, openUserSession, validationResult.entryPoint(), authenticatedCredentialsResponse, acquirationKey) //
				.map(userSession -> createResponseFrom(userSession, false));
	}

	private record ValidationResult(OpenUserSessionEntryPoint entryPoint) {
	}

	private class RequestValidationContext {
		private final Lazy<UserSession> userSession;

		RequestValidationContext(ServiceRequestContext requestContext) {
			userSession = new Lazy<>(() -> requestContext.findOrNull(UserSessionAspect.class));
		}

		boolean isInternalRequest() {
			UserSession session = userSession.get();
			return session != null && session.getEffectiveRoles().contains(internalRole);
		}
	}

	private Maybe<ValidationResult> validate(ServiceRequestContext requestContext, OpenUserSession request) {
		if (request.getCredentials() == null)
			return Reasons.build(InvalidArgument.T).text("OpenUserSession.credentials must not be null").toMaybe();

		RequestValidationContext validationContext = new RequestValidationContext(requestContext);
		if (request.getSkipAuthorization() && !validationContext.isInternalRequest()) {
			Forbidden cause = Reasons.build(Forbidden.T).text("Insufficient rights to skip authorization").toReason();
			return Reasons.build(InvalidArgument.T).text("Invalid property value for skipAuthorization").cause(cause).toMaybe();
		}

		String entryPointName = request.getEntryPoint();
		OpenUserSessionEntryPoint entryPoint;
		if (entryPointName != null) {
			if (!validationContext.isInternalRequest()) {
				Forbidden cause = Reasons.build(Forbidden.T).text("Insufficient rights to specify an entry point").toReason();
				return Reasons.build(InvalidArgument.T).text("Invalid property value for entryPoint").cause(cause).toMaybe();
			}

			entryPoint = entryPointsByName.get().get(entryPointName);
			if (entryPoint == null) {
				NotFound cause = Reasons.build(NotFound.T).text("Unknown entry point [" + entryPointName + "]").toReason();
				return Reasons.build(InvalidArgument.T).text("Invalid property value for entryPoint").cause(cause).toMaybe();
			}
		} else {
			entryPoint = requestContext.findOrNull(OpenUserSessionEntryPointAttribute.class);
		}

		return Maybe.complete(new ValidationResult(entryPoint));
	}

	private Reason checkAuthorization(OpenUserSessionEntryPoint entryPoint, Set<String> effectiveRoles) {
		if (entryPoint == null || checkAuthorization(effectiveRoles, entryPoint.getAllowedRoles(), entryPoint.getForbiddenRoles()))
			return null;

		return Reasons.build(Forbidden.T).text("Insufficient rights to open a UserSession this way").toReason();
	}

	public static boolean checkAuthorization(Set<String> effectiveRoles, Set<String> allowedRoles, Set<String> forbiddenRoles) {
		if (!forbiddenRoles.isEmpty() && effectiveRoles.stream().anyMatch(forbiddenRoles::contains))
			return false;
		if (!allowedRoles.isEmpty())
			return effectiveRoles.stream().anyMatch(allowedRoles::contains);
		return true;
	}

	private Maybe<UserSession> acquireUserSession(ServiceRequestContext requestContext, String acquirationKey) {
		return userSessionService.findUserSessionByAcquirationKey(acquirationKey) //
				.flatMap(s -> validateUserSession(requestContext, s)) //
				.ifSatisfied(this::touchUserSession);
	}

	private OpenUserSessionResponse createResponseFrom(UserSession userSession, boolean reused) {
		OpenUserSessionResponse response = OpenUserSessionResponse.T.create();
		response.setUserSession(userSession);
		response.setReused(reused);
		return response;
	}

	private Maybe<UserSession> buildUserSession(ServiceRequestContext context, OpenUserSession openUserSession, OpenUserSessionEntryPoint entryPoint,
			AuthenticateCredentialsResponse authenticatedCredentialsResponse, String acquirationKey) {

		if (authenticatedCredentialsResponse instanceof AuthenticatedUser authenticatedUser) {
			log.trace(() -> "Creating session for client from IP: " + context.getRequestorAddress());

			User user = authenticatedUser.getUser();

			if (authenticatedUser.getEnsureUserPersistence()) {
				Reason error = userService.ensureUser(user);
				String uuid = UUID.randomUUID().toString();
				String msg = "Error while ensuring User persistence (tracebackId=" + uuid + ")";
				if (error != null) {
					log.error(msg + ": " + error.stringify());
					return Reasons.build(InternalError.T).text(msg).toMaybe();
				}
			}

			// TODO: should the expiry date influenced from the outside via OpenUserSession.expiryDate or should this only be
			// controlled by Credentials/Authentication
			//@formatter:off
			Maybe<UserSession> userSessionMaybe = new BasicUserSessionBuilder(userSessionService, entryPoint, sessionMaxIdleTime, sessionMaxAge)
				.requestContext(context)
				.request(openUserSession)
				.acquirationKey(acquirationKey)
				.expiryDate(authenticatedUser.getExpiryDate())
				.addProperties(authenticatedUser.getProperties())
				.blocksAuthenticationAfterLogout(authenticatedUser.getInvalidateCredentialsOnLogout())
				.buildFor(user);
			//@formatter:on

			boolean satisfied = userSessionMaybe.isSatisfied();

			this.logAuthentication(context, openUserSession, satisfied);

			if (!satisfied)
				return userSessionMaybe.propagateReason();

			collectStatisticsUponLogin(userSessionMaybe.get());

			return userSessionMaybe;

		} else {
			return Reasons.build(InternalError.T)
					.text("Unsupported AuthenticateCredentialsResponse type: " + authenticatedCredentialsResponse.entityType().getTypeSignature())
					.toMaybe();
		}
	}

	protected void logAuthentication(ServiceRequestContext requestContext, OpenUserSession openUserSession, boolean successful) {
		if (!log.isDebugEnabled()) {
			return;
		}

		try {
			Credentials credentials = openUserSession.getCredentials();

			String type = null;
			String userId = null;
			if (credentials instanceof AbstractUserIdentificationCredentials) {
				type = "user";
				AbstractUserIdentificationCredentials upc = (AbstractUserIdentificationCredentials) credentials;
				UserIdentification userIdentification = upc.getUserIdentification();
				if (userIdentification instanceof UserNameIdentification) {
					userId = ((UserNameIdentification) userIdentification).getUserName();
				} else if (userIdentification instanceof EmailIdentification) {
					userId = ((EmailIdentification) userIdentification).getEmail();
				}
			} else if (credentials instanceof TokenWithUserNameCredentials) {
				type = "user";
				TokenWithUserNameCredentials tuc = (TokenWithUserNameCredentials) credentials;
				userId = tuc.getUserName();
			} else if (credentials instanceof ExistingSessionCredentials) {
				if (successful) {
					// Well, a session has been verified. No point in logging that.
					return;
				}
				type = "session";
				ExistingSessionCredentials esc = (ExistingSessionCredentials) credentials;
				userId = esc.getExistingSessionId();
			} else {
				log.trace(() -> "Not logging authentication of credentials " + credentials + " with success: " + successful);
				return;
			}

			if (userId != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("Authentication of ");
				sb.append(type);
				sb.append(" '");
				sb.append(userId);
				sb.append("' from '");

				String address = requestContext != null ? requestContext.getRequestorAddress() : null;
				sb.append(address);
				sb.append("' ");

				if (successful) {
					sb.append("succeeded");
				} else {
					sb.append("failed");
				}
				log.debug(sb.toString());
			}

		} catch (Exception e) {
			log.debug(() -> "Could not log an authentication attempt", e);
		}
	}

	private Maybe<UserSession> validateUserSession(ServiceRequestContext requestContext, ValidateUserSession request) {
		Maybe<UserSession> userSessionMaybe = userSessionService.findUserSession(request.getSessionId());

		return userSessionMaybe //
				.flatMap(s -> validateUserSession(requestContext, s)) //
				.ifSatisfied(this::touchUserSession);
	}

	private void touchUserSession(UserSession userSession) {
		Date lastAccessedDate = new Date();
		Date fixedExpiryDate = userSession.getFixedExpiryDate();

		TimeSpan maxIdleTime = userSession.getMaxIdleTime();

		Date expiryDate = calculateExpiryDate(lastAccessedDate, maxIdleTime, fixedExpiryDate);

		userSessionService.touchUserSession(userSession.getSessionId(), lastAccessedDate, expiryDate);
	}

	protected Date calculateExpiryDate(Date pivot, TimeSpan span, Date fixedExpiryDate) {
		if (span == null) {
			return fixedExpiryDate;
		}
		Date expiryDate = new Date(pivot.getTime() + span.toLongMillies());

		if (fixedExpiryDate != null && fixedExpiryDate.before(expiryDate))
			return fixedExpiryDate;

		return expiryDate;
	}

	private Maybe<UserSession> validateUserSession(ServiceRequestContext requestContext, UserSession userSession) {
		log.trace(() -> "Validating user session: " + userSession);

		Date expiryDate = userSession.getExpiryDate();

		if (expiryDate != null) {
			Date now = new Date();

			if (now.after(expiryDate)) {
				return Reasons.build(SessionExpired.T).text("User session '" + userSession.getSessionId() + "' has expired.").toMaybe();
			}
		}

		Lazy<Reason> verifyReason = new Lazy<>(
				() -> Reasons.build(InvalidSession.T).text("User session '" + userSession.getSessionId() + "' is invalid.").toReason());

		if (userSessionAccessVerificationExperts != null && !userSessionAccessVerificationExperts.isEmpty()) {
			for (UserSessionAccessVerificationExpert expert : userSessionAccessVerificationExperts) {
				Reason reason = expert.verifyUserSessionAccess(requestContext, userSession);

				if (reason != null)
					verifyReason.get().getReasons().add(reason);
			}
		}

		if (verifyReason.isInitialized()) {
			log.debug(verifyReason.get().stringify());
			return Reasons.build(InvalidSession.T).text("User session '" + userSession.getSessionId() + "' is invalid.").toMaybe();
		}

		return Maybe.complete(userSession);
	}

	private User getCurrentUser(ServiceRequestContext requestContext) {
		return requestContext.findAttribute(UserSessionAspect.class).map(UserSession::getUser).orElse(null);
	}

	private void collectStatisticsUponLogin(@SuppressWarnings("unused") UserSession userSession) {
		if (!enableUserStatistics)
			return;
		// TODO support user statistics
	}

	private void collectStatisticsUponLogout(@SuppressWarnings("unused") DeletedSessionInfo deletedSessionInfo) {
		if (!enableUserStatistics)
			return;
		// TODO support user statistics
	}
}
