package hiconic.rx.browser.acceptance.processing;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.ApprovalPending;
import com.braintribe.gm.model.security.reason.ApprovalRequired;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.browser.acceptance.model.BrowserAcceptance;
import hiconic.rx.browser.acceptance.model.BrowserAcceptanceState;
import hiconic.rx.browser.acceptance.model.api.RequestBrowserAcceptance;
import hiconic.rx.security.web.api.LoginFlowExtension;
import hiconic.rx.security.web.api.LoginIntervention;

/** Connects browser acceptance to the generic login flow without exposing feature-specific behavior to the login page. */
public class BrowserAcceptanceLoginFlowExtension implements LoginFlowExtension {
	public static final String REQUEST_APPROVAL = "request-device-browser-approval";

	private final Evaluator<ServiceRequest> evaluator;

	public BrowserAcceptanceLoginFlowExtension(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public LoginIntervention describe(Reason reason) {
		if (ApprovalPending.T.isInstance(reason))
			return pending();
		if (ApprovalRequired.T.isInstance(reason))
			return LoginIntervention.actionRequired("Device/browser approval required",
					"This device or browser must be approved before you can sign in.", REQUEST_APPROVAL, "Request approval");
		return null;
	}

	@Override
	public boolean supportsAction(String actionId) {
		return REQUEST_APPROVAL.equals(actionId);
	}

	@Override
	public Maybe<LoginIntervention> performAction(String actionId, OpenUserSessionWithUserAndPassword loginRequest, String entryPoint) {
		if (!supportsAction(actionId))
			throw new IllegalArgumentException("Unsupported login action: " + actionId);

		RequestBrowserAcceptance request = RequestBrowserAcceptance.T.create();
		request.setCredentials(UserPasswordCredentials.forUserName(loginRequest.getUser(), loginRequest.getPassword()));
		request.setEntryPoint(entryPoint);
		Maybe<BrowserAcceptance> result = request.eval(evaluator).getReasoned();
		if (result.isUnsatisfied())
			return result.propagateReason();

		BrowserAcceptance acceptance = result.get();
		return switch (acceptance.getState()) {
			case PENDING -> Maybe.complete(pending());
			case APPROVED -> Maybe.complete(LoginIntervention.retryAuthentication());
			case REJECTED, REVOKED -> authenticationFailed();
			case EXPIRED -> throw new IllegalStateException("An explicit approval request must renew an expired acceptance");
			case FORGOTTEN -> throw new IllegalStateException("An explicit approval request must create a new acceptance after forgetting");
		};
	}

	private static Maybe<LoginIntervention> authenticationFailed() {
		return Reasons.build(AuthenticationFailure.T)
				.text("Sign-in could not be completed. This browser or device is not authorized.")
				.toMaybe();
	}

	private static LoginIntervention pending() {
		return LoginIntervention.pending("Approval pending",
				"Your request was submitted. An administrator must approve this device or browser before you can sign in.");
	}
}
