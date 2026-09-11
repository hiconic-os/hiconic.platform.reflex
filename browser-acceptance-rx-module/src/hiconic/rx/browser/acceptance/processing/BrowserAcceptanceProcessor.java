package hiconic.rx.browser.acceptance.processing;

import java.util.Set;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;
import com.braintribe.model.usersession.UserSession;
import hiconic.rx.browser.acceptance.model.BrowserAcceptance;
import hiconic.rx.browser.acceptance.model.BrowserAcceptanceState;
import hiconic.rx.browser.acceptance.model.api.ApproveBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.BrowserAcceptanceRequest;
import hiconic.rx.browser.acceptance.model.api.BrowserAcceptances;
import hiconic.rx.browser.acceptance.model.api.ChangeBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.ListBrowserAcceptances;
import hiconic.rx.browser.acceptance.model.api.RejectBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.RevokeBrowserAcceptance;

public class BrowserAcceptanceProcessor extends AbstractDispatchingServiceProcessor<BrowserAcceptanceRequest, Object> {
	private final BrowserAcceptanceStore store;
	private final Set<String> approverRoles;

	public BrowserAcceptanceProcessor(BrowserAcceptanceStore store, Set<String> approverRoles) {
		this.store = store;
		this.approverRoles = approverRoles;
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
