package hiconic.rx.browser.acceptance.processing;

import java.util.Set;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.ApprovalRequired;
import com.braintribe.gm.model.security.reason.BrowserContextExpired;
import com.braintribe.gm.model.security.reason.BrowserContextRejected;
import com.braintribe.gm.model.security.reason.BrowserContextRevoked;
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
		return verify(context, entryPoint, userIdentity(user), user.getName());
	}

	@Override
	public Reason verifyUserSessionAccess(ServiceRequestContext context, UserSession session) {
		String entryPoint = session.getProperties().get("openUserSession.entryPoint");
		if (!policies.applies(entryPoint, session.getEffectiveRoles()))
			return null;
		return verify(context, entryPoint, userIdentity(session.getUser()), session.getUser().getName());
	}

	private String userIdentity(User user) {
		/* Existing sessions created before user ids were persisted still need a stable fallback. */
		return user.getId() != null ? user.getId().toString() : user.getName();
	}

	private Reason verify(ServiceRequestContext context, String entryPoint, String userId, String userName) {
		String rawToken = context.findOrNull(BrowserContextIdAttribute.class);
		if (rawToken == null)
			return Reasons.build(ApprovalRequired.T).text("Approval is required").toReason();
		BrowserAcceptance acceptance = store.findOrRequest(rawToken, userId, userName, entryPoint, context.getRequestorAddress(),
				context.findOrNull(BrowserRequestInformationAttribute.class));
		return switch (acceptance.getState()) {
			case APPROVED -> null;
			case REJECTED -> Reasons.build(BrowserContextRejected.T).text("Browser context was rejected").toReason();
			case REVOKED -> Reasons.build(BrowserContextRevoked.T).text("Browser context was revoked").toReason();
			case EXPIRED -> Reasons.build(BrowserContextExpired.T).text("Browser context has expired").toReason();
			case PENDING -> Reasons.build(ApprovalRequired.T).text("Approval is required")
					.enrich(r -> {
						r.setApprovalRequestId(acceptance.getId());
						r.setExpiryDate(acceptance.getExpiresAt());
					}).toReason();
		};
	}
}
