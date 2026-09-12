package hiconic.rx.browser.acceptance.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.security.reason.ApprovalRequired;
import com.braintribe.gm.model.security.reason.ApprovalPending;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.model.processing.securityservice.api.attributes.OpenUserSessionEntryPointAttribute;
import com.braintribe.model.processing.service.api.SessionIdAspect;
import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;
import com.braintribe.model.securityservice.ValidateUserSession;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.browser.acceptance.model.BrowserAcceptance;
import hiconic.rx.browser.acceptance.model.BrowserAcceptanceState;
import hiconic.rx.browser.acceptance.model.api.ApproveBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.BrowserAcceptances;
import hiconic.rx.browser.acceptance.model.api.ForgetBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.ListBrowserAcceptances;
import hiconic.rx.browser.acceptance.model.api.RevokeBrowserAcceptance;
import hiconic.rx.browser.acceptance.model.api.RequestBrowserAcceptance;
import hiconic.rx.browser.acceptance.processing.BrowserContextIdAttribute;
import hiconic.rx.browser.acceptance.processing.BrowserRequestInformation;
import hiconic.rx.browser.acceptance.processing.BrowserRequestInformationAttribute;
import hiconic.rx.browser.acceptance.processing.BrowserAcceptanceLoginFlowExtension;
import hiconic.rx.security.web.api.LoginIntervention;
import hiconic.rx.security.web.api.LoginInterventionState;
import hiconic.rx.test.common.AbstractRxTest;

public class BrowserAcceptancePlatformTest extends AbstractRxTest {
	private static final OpenUserSessionEntryPoint CLAIMSDESK_ENTRY_POINT = entryPoint("claimsdesk", "claimsdesk-user");
	private static final OpenUserSessionEntryPoint PLATFORM_ENTRY_POINT = entryPoint("platform", "platform-user");

    @Test
    public void requiresApprovalOnlyAfterValidCredentialsAndAllowsLoginAfterApproval() {
        String browserToken = "browser-token";
		BrowserRequestInformation browserInformation = new BrowserRequestInformation("10.0.0.7", "Test Browser/1.0",
				"\"Test Browser\";v=\"1\"", "\"Linux\"", "?0");

        Maybe<? extends OpenUserSessionResponse> invalid = withBrowser(browserToken,
                () -> login(PLATFORM_ENTRY_POINT, "subject", "wrong-password"));
        assertThat(invalid.isUnsatisfiedBy(InvalidCredentials.T)).isTrue();

        Maybe<? extends OpenUserSessionResponse> claimsdeskLogin = withBrowser("unapproved-claimsdesk-browser",
                () -> login(CLAIMSDESK_ENTRY_POINT, "claimsdesk-subject", "claimsdesk-password"));
        assertThat(claimsdeskLogin.isSatisfied()).isTrue();

		Maybe<? extends OpenUserSessionResponse> approverLogin = withBrowser("approver-browser",
				() -> login(PLATFORM_ENTRY_POINT, "approver", "approver-password"));
		assertThat(approverLogin.isSatisfied())
				.withFailMessage(() -> "Approver login failed: " + approverLogin.whyUnsatisfied().stringify())
				.isTrue();
		OpenUserSessionResponse approverSession = approverLogin.get();
        BrowserAcceptances beforeRequest = authenticated(approverSession,
                () -> ListBrowserAcceptances.T.create().eval(evaluator).get());
        assertThat(beforeRequest.getAcceptances()).isEmpty();

		Maybe<? extends OpenUserSessionResponse> approvalRequired = withBrowser(browserToken, browserInformation,
                () -> login(PLATFORM_ENTRY_POINT, "subject", "subject-password"));
		assertThat(approvalRequired.isUnsatisfiedBy(ApprovalRequired.T)).isTrue();
		assertThat(approvalRequired.isUnsatisfiedBy(ApprovalPending.T)).isFalse();
		assertThat(authenticated(approverSession, () -> ListBrowserAcceptances.T.create().eval(evaluator).get()).getAcceptances()).isEmpty();
		BrowserAcceptanceLoginFlowExtension loginFlow = new BrowserAcceptanceLoginFlowExtension(evaluator);
		LoginIntervention requiredIntervention = loginFlow.describe(approvalRequired.whyUnsatisfied());
		assertThat(requiredIntervention.state()).isEqualTo(LoginInterventionState.ACTION_REQUIRED);
		assertThat(requiredIntervention.actionId()).isEqualTo(BrowserAcceptanceLoginFlowExtension.REQUEST_APPROVAL);

		Maybe<BrowserAcceptance> invalidRequest = withBrowser(browserToken,
				() -> requestApproval(PLATFORM_ENTRY_POINT, "subject", "wrong-password"));
		assertThat(invalidRequest.isUnsatisfiedBy(InvalidCredentials.T))
				.withFailMessage(() -> invalidRequest.isSatisfied()
						? "Invalid credentials unexpectedly created an acceptance: " + invalidRequest.get()
						: "Unexpected explicit approval failure: " + invalidRequest.whyUnsatisfied().stringify())
				.isTrue();
		assertThat(authenticated(approverSession, () -> ListBrowserAcceptances.T.create().eval(evaluator).get()).getAcceptances()).isEmpty();

		OpenUserSessionWithUserAndPassword loginRequest = OpenUserSessionWithUserAndPassword.T.create();
		loginRequest.setUser("subject");
		loginRequest.setPassword("subject-password");
		Maybe<LoginIntervention> requested = withBrowser(browserToken, browserInformation,
				() -> loginFlow.performAction(BrowserAcceptanceLoginFlowExtension.REQUEST_APPROVAL, loginRequest,
						PLATFORM_ENTRY_POINT.getName()));
		assertThat(requested.isSatisfied()).isTrue();
		assertThat(requested.get().state()).isEqualTo(LoginInterventionState.PENDING);

		Maybe<? extends OpenUserSessionResponse> pendingLogin = withBrowser(browserToken,
				() -> login(PLATFORM_ENTRY_POINT, "subject", "subject-password"));
		assertThat(pendingLogin.isUnsatisfiedBy(ApprovalPending.T)).isTrue();

        BrowserAcceptance pending = authenticated(approverSession, () -> {
            ListBrowserAcceptances request = ListBrowserAcceptances.T.create();
            request.setState(BrowserAcceptanceState.PENDING);
            return request.eval(evaluator).get().getAcceptances().get(0);
        });
        assertThat(pending.getUserId()).isEqualTo("subject-id");
		assertThat(pending.getDirectRequestorAddress()).isEqualTo("10.0.0.7");
		assertThat(pending.getLastDirectRequestorAddress()).isEqualTo("10.0.0.7");
		assertThat(pending.getUserAgent()).isEqualTo("Test Browser/1.0");
		assertThat(pending.getClientHintsUserAgent()).isEqualTo("\"Test Browser\";v=\"1\"");
		assertThat(pending.getClientHintsPlatform()).isEqualTo("\"Linux\"");
		assertThat(pending.getClientHintsMobile()).isEqualTo("?0");
		Maybe<BrowserAcceptance> repeatedRequest = withBrowser(browserToken,
				() -> requestApproval(PLATFORM_ENTRY_POINT, "subject", "subject-password"));
		assertThat(repeatedRequest.isSatisfied()).isTrue();
		String repeatedRequestId = repeatedRequest.get().getId();
		assertThat(repeatedRequestId).isEqualTo(pending.getId());
		assertThat(authenticated(approverSession, () -> ListBrowserAcceptances.T.create().eval(evaluator).get()).getAcceptances()).hasSize(1);

        BrowserAcceptance approved = authenticated(approverSession, () -> {
            ApproveBrowserAcceptance request = ApproveBrowserAcceptance.T.create();
            request.setAcceptanceId(pending.getId());
            return request.eval(evaluator).get();
        });
        assertThat(approved.getState()).isEqualTo(BrowserAcceptanceState.APPROVED);

		Maybe<? extends OpenUserSessionResponse> acceptedLogin = withBrowser(browserToken,
				() -> login(PLATFORM_ENTRY_POINT, "subject", "subject-password"));
		assertThat(acceptedLogin.isSatisfied()).isTrue();

		ValidateUserSession validate = ValidateUserSession.T.create();
		validate.setSessionId(acceptedLogin.get().getUserSession().getSessionId());
		assertThat(withBrowser(browserToken, () -> validate.eval(evaluator).getReasoned()).isSatisfied()).isTrue();

		authenticated(approverSession, () -> {
			RevokeBrowserAcceptance request = RevokeBrowserAcceptance.T.create();
			request.setAcceptanceId(pending.getId());
			return request.eval(evaluator).get();
		});
		Maybe<? extends OpenUserSessionResponse> revokedLogin = withBrowser(browserToken,
				() -> login(PLATFORM_ENTRY_POINT, "subject", "subject-password"));
		assertThat(revokedLogin.isUnsatisfiedBy(AuthenticationFailure.T)).isTrue();
		assertThat(revokedLogin.whyUnsatisfied().entityType()).isEqualTo(AuthenticationFailure.T);
		assertThat(revokedLogin.whyUnsatisfied().getText())
				.isEqualTo("Sign-in could not be completed. This browser or device is not authorized.");
		Maybe<BrowserAcceptance> requestAfterRevocation = withBrowser(browserToken,
				() -> requestApproval(PLATFORM_ENTRY_POINT, "subject", "subject-password"));
		assertThat(requestAfterRevocation.isSatisfied()).isTrue();
		assertThat(requestAfterRevocation.get().getState()).isEqualTo(BrowserAcceptanceState.REVOKED);
		BrowserAcceptances noPendingRequest = authenticated(approverSession, () -> {
			ListBrowserAcceptances request = ListBrowserAcceptances.T.create();
			request.setState(BrowserAcceptanceState.PENDING);
			return request.eval(evaluator).get();
		});
		assertThat(noPendingRequest.getAcceptances()).isEmpty();

		BrowserAcceptance forgotten = authenticated(approverSession, () -> {
			ForgetBrowserAcceptance request = ForgetBrowserAcceptance.T.create();
			request.setAcceptanceId(pending.getId());
			return request.eval(evaluator).get();
		});
		assertThat(forgotten.getState()).isEqualTo(BrowserAcceptanceState.FORGOTTEN);
		assertThat(withBrowser(browserToken, () -> login(PLATFORM_ENTRY_POINT, "subject", "subject-password"))
				.isUnsatisfiedBy(ApprovalRequired.T)).isTrue();
		Maybe<BrowserAcceptance> newRequest = withBrowser(browserToken,
				() -> requestApproval(PLATFORM_ENTRY_POINT, "subject", "subject-password"));
		assertThat(newRequest.isSatisfied()).isTrue();
		assertThat(newRequest.get().getState()).isEqualTo(BrowserAcceptanceState.PENDING);
		assertThat(newRequest.get().getId()).isEqualTo(pending.getId());

		BrowserAcceptances completeHistory = authenticated(approverSession,
				() -> ListBrowserAcceptances.T.create().eval(evaluator).get());
		assertThat(completeHistory.getAcceptances()).hasSize(1);
		BrowserAcceptance reactivated = completeHistory.getAcceptances().get(0);
		assertThat(reactivated.getState()).isEqualTo(BrowserAcceptanceState.PENDING);
		assertThat(reactivated.getEvents()).extracting(event -> event.getType().name())
				.containsExactly("REQUESTED", "APPROVED", "REVOKED", "FORGOTTEN", "REQUESTED");
		assertThat(withBrowser("new-browser-token", () -> login(PLATFORM_ENTRY_POINT, "subject", "subject-password"))
				.isUnsatisfiedBy(ApprovalRequired.T)).isTrue();
	}

    private Maybe<? extends OpenUserSessionResponse> login(OpenUserSessionEntryPoint entryPoint, String user, String password) {
        OpenUserSessionWithUserAndPassword request = OpenUserSessionWithUserAndPassword.T.create();
        request.setUser(user);
        request.setPassword(password);
        return AttributeContexts.derivePeek()
                .set(OpenUserSessionEntryPointAttribute.class, entryPoint)
                .buildAnd().execute(() -> request.eval(evaluator).getReasoned());
    }

	private Maybe<BrowserAcceptance> requestApproval(OpenUserSessionEntryPoint entryPoint, String user, String password) {
		RequestBrowserAcceptance request = RequestBrowserAcceptance.T.create();
		request.setCredentials(UserPasswordCredentials.forUserName(user, password));
		request.setEntryPoint(entryPoint.getName());
		return request.eval(evaluator).getReasoned();
	}

	private static OpenUserSessionEntryPoint entryPoint(String name, String allowedRole) {
		OpenUserSessionEntryPoint entryPoint = OpenUserSessionEntryPoint.T.create();
		entryPoint.setName(name);
		entryPoint.getAllowedRoles().add(allowedRole);
		return entryPoint;
	}

    private <T> T withBrowser(String token, Supplier<T> action) {
        return AttributeContexts.derivePeek().set(BrowserContextIdAttribute.class, token).buildAnd().execute(action);
    }

	private <T> T withBrowser(String token, BrowserRequestInformation information, Supplier<T> action) {
		return AttributeContexts.derivePeek()
				.set(BrowserContextIdAttribute.class, token)
				.set(BrowserRequestInformationAttribute.class, information)
				.buildAnd().execute(action);
	}

    private <T> T authenticated(OpenUserSessionResponse response, Supplier<T> action) {
        return AttributeContexts.derivePeek()
                .set(SessionIdAspect.class, response.getUserSession().getSessionId())
                .buildAnd().execute(action);
    }
}
