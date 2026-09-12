package hiconic.rx.security.web.api;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;

/**
 * Extends the platform login flow with declarative failure presentation and explicitly requested server-side actions.
 */
public interface LoginFlowExtension {

	/** Returns a presentation for a supported reason, or {@code null}. */
	LoginIntervention describe(Reason reason);

	/** Whether this extension owns the given action identifier. */
	boolean supportsAction(String actionId);

	/** Performs the action after the generic login transport decoded it. Implementations must re-authenticate the credentials. */
	Maybe<LoginIntervention> performAction(String actionId, OpenUserSessionWithUserAndPassword loginRequest, String entryPoint);
}
