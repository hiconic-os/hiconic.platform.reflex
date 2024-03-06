// ============================================================================
package hiconic.platform.reflex.security.processor;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.InternalError;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.gm.model.security.reason.InvalidSession;
import com.braintribe.gm.model.security.reason.SessionExpired;
import com.braintribe.gm.model.security.reason.SessionNotFound;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.securityservice.api.DeletedSessionInfo;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
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
import com.braintribe.utils.lcd.LazyInitialized;

import hiconic.platform.reflex.security.api.UserInternalService;
import hiconic.platform.reflex.security.api.UserSessionAccessVerificationExpert;

public class SecurityServiceProcessor extends AbstractDispatchingServiceProcessor<SecurityRequest, Object> {

	private static final Logger log = Logger.getLogger(SecurityServiceProcessor.class);

	private UserInternalService userService;
	private UserSessionService userSessionService;
	private Evaluator<ServiceRequest> evaluator;
	private List<UserSessionAccessVerificationExpert> userSessionAccessVerificationExperts;

	private boolean enableUserStatistics;
	private TimeSpan sessionMaxIdleTime;
	private TimeSpan sessionMaxAge;
	private CredentialsHasher credentialsHasher = new CredentialsHasher();
	
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
	public void setUserService(UserInternalService userService) {
		this.userService = userService;
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
		dispatching.registerReasoned(OpenUserSession.T, this::openUserSession);
		dispatching.registerReasoned(ValidateUserSession.T, this::validateUserSession);
		dispatching.register(GetCurrentUser.T, this::getCurrentUser);
		dispatching.register(Logout.T, this::logout);
		dispatching.register(LogoutSession.T, this::logoutSession);
	}

	public UserInternalService getUserInternalService() {
		return userService;
	}

	private boolean logoutSession(ServiceRequestContext context, LogoutSession request) {
		return logout(request.getSessionId());
	}

	private boolean logout(ServiceRequestContext context, Logout request) {
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
		Maybe<UserSession> maybe = openOrAquireUserSession(requestContext, openUserSession);
		return maybe.map(this::createResponseFrom);
	}

	private Maybe<UserSession> openOrAquireUserSession(ServiceRequestContext requestContext, OpenUserSession openUserSession) {
		Credentials credentials = openUserSession.getCredentials();

		if (credentials == null)
			return Reasons.build(InvalidArgument.T).text("OpenUserSession.credentials must not be null").toMaybe();

		String acquirationKey = null;

		if (credentials.acquirationSupportive()) {
			// !
			acquirationKey = credentialsHasher.hash(credentials, m -> requestContext.getRequestorAddress());

			Maybe<UserSession> acquiredUserSessionMaybe = acquireUserSession(requestContext, acquirationKey);

			if (acquiredUserSessionMaybe.isSatisfied()) {
				return acquiredUserSessionMaybe;
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

		return buildUserSession(requestContext, openUserSession, maybe.get(), acquirationKey);
	}

	private Maybe<UserSession> acquireUserSession(ServiceRequestContext requestContext, String acquirationKey) {
		return userSessionService.findUserSessionByAcquirationKey(acquirationKey) //
				.flatMap(s -> validateUserSession(requestContext, s)) //
				.ifSatisfied(this::touchUserSession);
	}

	private OpenUserSessionResponse createResponseFrom(UserSession userSession) {
		OpenUserSessionResponse response = OpenUserSessionResponse.T.create();
		response.setUserSession(userSession);
		return response;
	}

	private Maybe<UserSession> buildUserSession(ServiceRequestContext context, OpenUserSession openUserSession,
			AuthenticateCredentialsResponse authenticatedCredentialsResponse, String acquirationKey) {
		if (authenticatedCredentialsResponse instanceof AuthenticatedUser) {
			AuthenticatedUser authenticatedUser = (AuthenticatedUser) authenticatedCredentialsResponse;

			log.trace(() -> "Creating session for client from IP: " + context.getRequestorAddress());

			User user = authenticatedUser.getUser();

			if (authenticatedUser.getEnsureUserPersistence()) {
				Reason error = getUserInternalService().ensureUser(user);
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
			Maybe<UserSession> userSessionMaybe = new BasicUserSessionBuilder(userSessionService, sessionMaxIdleTime, sessionMaxAge)
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
				return Maybe.empty(userSessionMaybe.whyUnsatisfied());

			collectStatisticsUponLogin(userSessionMaybe.get());

			return userSessionMaybe;

		} else if (authenticatedCredentialsResponse instanceof AuthenticatedUserSession) {
			AuthenticatedUserSession authenticatedUserSession = (AuthenticatedUserSession) authenticatedCredentialsResponse;
			return Maybe.complete(authenticatedUserSession.getUserSession());
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
				return Reasons.build(SessionExpired.T).text("User session '" + userSession.getId() + "' has expired.").toMaybe();
			}
		}

		LazyInitialized<Reason> verifyReason = new LazyInitialized<>(
				() -> Reasons.build(InvalidSession.T).text("User session '" + userSession.getId() + "' is invalid.").toReason());

		if (userSessionAccessVerificationExperts != null && !userSessionAccessVerificationExperts.isEmpty()) {
			for (UserSessionAccessVerificationExpert expert : userSessionAccessVerificationExperts) {
				Reason reason = expert.verifyUserSessionAccess(requestContext, userSession);

				if (reason != null)
					verifyReason.get().getReasons().add(reason);
			}
		}

		if (verifyReason.isInitialized()) {
			log.debug(verifyReason.get().stringify());
			return Reasons.build(InvalidSession.T).text("User session '" + userSession.getId() + "' is invalid.").toMaybe();
		}

		return Maybe.complete(userSession);
	}

	private User getCurrentUser(ServiceRequestContext requestContext, GetCurrentUser request) {
		return requestContext.findAttribute(UserSessionAspect.class).map(UserSession::getUser).orElse(null);
	}

	private void collectStatisticsUponLogin(UserSession userSession) {
		if (!enableUserStatistics)
			return;

	}

	private void collectStatisticsUponLogout(DeletedSessionInfo deletedSessionInfo) {
		if (!enableUserStatistics)
			return;

	}
}
