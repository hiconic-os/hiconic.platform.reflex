package hiconic.rx.browser.acceptance.processing;

import java.util.Set;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.ApprovalRequired;
import com.braintribe.gm.model.security.reason.ApprovalPending;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import hiconic.rx.browser.acceptance.model.BrowserAcceptance;
import hiconic.rx.browser.acceptance.model.BrowserAcceptanceState;
import hiconic.rx.security.api.UserSessionAccessVerificationExpert;
import hiconic.rx.security.api.UserSessionOpeningVerificationExpert;

public class BrowserAcceptanceVerifier implements UserSessionOpeningVerificationExpert, UserSessionAccessVerificationExpert {
	private final BrowserAcceptancePolicyMatcher policies;
	private final BrowserAcceptanceStore store;

	public BrowserAcceptanceVerifier(BrowserAcceptancePolicyMatcher policies, BrowserAcceptanceStore store) {
		this.policies = policies;
		this.store = store;
	}

	@Override
	public Reason verifyUserSessionOpening(ServiceRequestContext context, String entryPoint, User user, Set<String> roles) {
		if (!policies.applies(entryPoint, roles))
			return null;
		return verify(context, entryPoint, userIdentity(user));
	}

	@Override
	public Reason verifyUserSessionAccess(ServiceRequestContext context, UserSession session) {
		String entryPoint = session.getProperties().get("openUserSession.entryPoint");
		if (!policies.applies(entryPoint, session.getEffectiveRoles()))
			return null;
		return verify(context, entryPoint, userIdentity(session.getUser()));
	}

	private String userIdentity(User user) {
		/* Existing sessions created before user ids were persisted still need a stable fallback. */
		return user.getId() != null ? user.getId().toString() : user.getName();
	}

	private Reason verify(ServiceRequestContext context, String entryPoint, String userId) {
		String rawToken = context.findOrNull(BrowserContextIdAttribute.class);
		if (rawToken == null)
			return Reasons.build(ApprovalRequired.T).text("Approval is required").toReason();
		BrowserAcceptance acceptance = store.find(rawToken, userId, entryPoint, context.getRequestorAddress(),
				context.findOrNull(BrowserRequestInformationAttribute.class));
		if (acceptance == null || acceptance.getState() == BrowserAcceptanceState.EXPIRED)
			return Reasons.build(ApprovalRequired.T).text("Device/browser approval is required").toReason();
		return switch (acceptance.getState()) {
			case APPROVED -> null;
			case REJECTED, REVOKED -> authenticationFailed();
			case EXPIRED -> throw new IllegalStateException("Expired acceptance handled above");
			case FORGOTTEN -> throw new IllegalStateException("Forgotten acceptances must not participate in active lookup");
			case PENDING -> Reasons.build(ApprovalPending.T).text("Device/browser approval is pending")
					.enrich(r -> {
						r.setApprovalRequestId(acceptance.getId());
						r.setExpiryDate(acceptance.getExpiresAt());
					}).toReason();
		};
	}

	private static Reason authenticationFailed() {
		return Reasons.build(AuthenticationFailure.T)
				.text("Sign-in could not be completed. This browser or device is not authorized.")
				.toReason();
	}
}
