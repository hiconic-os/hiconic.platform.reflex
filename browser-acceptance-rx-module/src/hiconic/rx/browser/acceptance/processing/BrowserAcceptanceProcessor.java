package hiconic.rx.browser.acceptance.processing;

import java.util.Set;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.securityservice.impl.Roles;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.AuthenticatedUser;
import com.braintribe.model.securityservice.AuthenticatedUserSession;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.user.User;
import hiconic.rx.browser.acceptance.model.BrowserAcceptance;
import hiconic.rx.browser.acceptance.model.BrowserAcceptanceState;
import hiconic.rx.browser.acceptance.model.api.ApproveBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.BrowserAcceptanceRequest;
import hiconic.rx.browser.acceptance.model.api.BrowserAcceptances;
import hiconic.rx.browser.acceptance.model.api.ChangeBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.ListBrowserAcceptances;
import hiconic.rx.browser.acceptance.model.api.RejectBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.RevokeBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.RequestBrowserAcceptance;

public class BrowserAcceptanceProcessor extends AbstractDispatchingServiceProcessor<BrowserAcceptanceRequest, Object> {
	private final BrowserAcceptanceStore store;
	private final Set<String> approverRoles;
	private final BrowserAcceptancePolicyMatcher policies;
	private final Evaluator<ServiceRequest> evaluator;

	public BrowserAcceptanceProcessor(BrowserAcceptanceStore store, Set<String> approverRoles, BrowserAcceptancePolicyMatcher policies,
			Evaluator<ServiceRequest> evaluator) {
		this.store = store;
		this.approverRoles = approverRoles;
		this.policies = policies;
		this.evaluator = evaluator;
	}

	/** This unauthenticated request performs its own credential authentication before creating any persistent state. */
	public Maybe<BrowserAcceptance> request(ServiceRequestContext context, RequestBrowserAcceptance request) {
		if (request.getCredentials() == null)
			return Reasons.build(InvalidArgument.T).text("RequestBrowserAcceptance.credentials must not be null").toMaybe();

		AuthenticateCredentials authentication = AuthenticateCredentials.T.create();
		authentication.setCredentials(request.getCredentials());
		Maybe<? extends AuthenticateCredentialsResponse> authenticationResult = authentication.eval(evaluator).getReasoned();
		if (authenticationResult.isUnsatisfied())
			return authenticationResult.propagateReason();

		AuthenticateCredentialsResponse response = authenticationResult.get();
		User user = authenticatedUser(response);
		Set<String> roles = Roles.authenticatedCredentialsEffectiveRoles(response);
		if (user == null)
			return Reasons.build(Forbidden.T).text("Credentials did not identify a user eligible for device/browser approval").toMaybe();
		if (!policies.applies(request.getEntryPoint(), roles))
			return Reasons.build(Forbidden.T).text("Device/browser approval does not apply to this login").toMaybe();

		String rawToken = context.findOrNull(BrowserContextIdAttribute.class);
		if (rawToken == null || rawToken.isBlank())
			return Reasons.build(InvalidArgument.T).text("No browser context is available for approval").toMaybe();

		String userId = user.getId() != null ? user.getId().toString() : user.getName();
		BrowserAcceptance acceptance = store.findOrRequest(rawToken, userId, user.getName(), request.getEntryPoint(),
				context.getRequestorAddress(), context.findOrNull(BrowserRequestInformationAttribute.class));
		return Maybe.complete(acceptance);
	}

	private static User authenticatedUser(AuthenticateCredentialsResponse response) {
		if (response instanceof AuthenticatedUser authenticatedUser)
			return authenticatedUser.getUser();
		if (response instanceof AuthenticatedUserSession authenticatedSession && authenticatedSession.getUserSession() != null)
			return authenticatedSession.getUserSession().getUser();
		return null;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<BrowserAcceptanceRequest, Object> dispatching) {
		dispatching.registerReasoned(ListBrowserAcceptances.T, this::list);
		dispatching.registerReasoned(ApproveBrowserAcceptance.T,
				(context, request) -> change(context, request, BrowserAcceptanceState.APPROVED));
		dispatching.registerReasoned(RejectBrowserAcceptance.T,
				(context, request) -> change(context, request, BrowserAcceptanceState.REJECTED));
		dispatching.registerReasoned(RevokeBrowserAcceptance.T,
				(context, request) -> change(context, request, BrowserAcceptanceState.REVOKED));
	}

	private Maybe<BrowserAcceptances> list(ServiceRequestContext context, ListBrowserAcceptances request) {
		Maybe<UserSession> approver = approver(context);
		if (approver.isUnsatisfied())
			return approver.propagateReason();

		BrowserAcceptances result = BrowserAcceptances.T.create();
		result.setAcceptances(store.list(request.getState()));
		return Maybe.complete(result);
	}

	private Maybe<BrowserAcceptance> change(ServiceRequestContext context, ChangeBrowserAcceptance request, BrowserAcceptanceState state) {
		Maybe<UserSession> approver = approver(context);
		if (approver.isUnsatisfied())
			return approver.propagateReason();

		UserSession session = approver.get();
		Object actorId = session.getUser().getId();
		String actorUserId = actorId == null ? session.getUser().getName() : actorId.toString();
		BrowserAcceptance result = store.change(request.getAcceptanceId(), state, actorUserId, context.getRequestorAddress());
		return result == null
				? Reasons.build(NotFound.T).text("Browser acceptance not found: " + request.getAcceptanceId()).toMaybe()
				: Maybe.complete(result);
	}

	private Maybe<UserSession> approver(ServiceRequestContext context) {
		UserSession session = context.findOrNull(UserSessionAspect.class);
		if (session == null || session.getEffectiveRoles().stream().noneMatch(approverRoles::contains))
			return Reasons.build(Forbidden.T).text("Browser acceptance approval role required").toMaybe();
		return Maybe.complete(session);
	}
}
