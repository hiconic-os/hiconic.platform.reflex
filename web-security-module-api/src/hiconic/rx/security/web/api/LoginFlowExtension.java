package hiconic.rx.security.web.api;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.securityservice.credentials.identification.UserNameIdentification;

/**
 * Extends the platform login flow with declarative failure presentation and explicitly requested server-side actions.
 */
public interface LoginFlowExtension {

	/** Returns a presentation for a supported reason, or {@code null}. */
	LoginIntervention describe(Reason reason);

	/** Whether this extension owns the given action identifier. */
	boolean supportsAction(String actionId);

	/**
	 * Performs the action after the generic login transport decoded the credentials. Implementations must re-authenticate the credentials.
	 * The credentials are deliberately independent of the concrete login mechanism (for example user/password or JWT).
	 */
	default Maybe<LoginIntervention> performAction(String actionId, Credentials credentials, String entryPoint) {
		if (credentials instanceof UserPasswordCredentials userPassword
				&& userPassword.getUserIdentification() instanceof UserNameIdentification userName) {
			OpenUserSessionWithUserAndPassword legacyRequest = OpenUserSessionWithUserAndPassword.T.create();
			legacyRequest.setUser(userName.getUserName());
			legacyRequest.setPassword(userPassword.getPassword());
			return performAction(actionId, legacyRequest, entryPoint);
		}

		return Reasons.build(UnsupportedOperation.T)
				.text("The login flow extension does not support credentials of type " + credentials.entityType().getTypeSignature())
				.toMaybe();
	}

	/**
	 * Compatibility adapter for extensions implemented before credential-neutral login actions were introduced.
	 * New extensions should override {@link #performAction(String, Credentials, String)}.
	 */
	@Deprecated
	default Maybe<LoginIntervention> performAction(String actionId, OpenUserSessionWithUserAndPassword loginRequest, String entryPoint) {
		return Reasons.build(UnsupportedOperation.T).text("The login flow extension does not implement action " + actionId).toMaybe();
	}
}
