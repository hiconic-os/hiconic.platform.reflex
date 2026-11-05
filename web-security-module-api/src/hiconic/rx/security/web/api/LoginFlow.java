package hiconic.rx.security.web.api;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.securityservice.credentials.Credentials;

/** Shared orchestration for login interventions, independent of the concrete authentication transport. */
public interface LoginFlow {

	/** Returns a presentation for a supported reason, or {@code null}. */
	LoginIntervention describe(Reason reason);

	/** Performs a registered login action with freshly obtained credentials. */
	Maybe<LoginIntervention> performAction(String actionId, Credentials credentials, String entryPoint);
}
