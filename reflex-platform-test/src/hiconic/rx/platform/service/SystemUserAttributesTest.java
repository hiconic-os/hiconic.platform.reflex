package hiconic.rx.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;

import com.braintribe.common.attribute.common.UserInfo;
import com.braintribe.common.attribute.common.UserInfoAttribute;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.model.processing.securityservice.api.attributes.LenientAuthenticationFailure;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.IsAuthorizedAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorSessionIdAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorUserNameAspect;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.platform.service.model.CheckSystemUserAttributes;
import hiconic.rx.test.common.AbstractRxTest;

public class SystemUserAttributesTest extends AbstractRxTest {
	@Test
	public void systemEvaluatorProvidesCompleteIdentity() {
		evaluateCheck();
	}

	@Test
	public void systemEvaluatorReplacesInheritedIdentityAndClearsAuthenticationFailure() {
		User user = User.T.create();
		user.setName("previous-user");
		UserSession previousSession = UserSession.T.create();
		previousSession.setUser(user);
		previousSession.setSessionId("previous-session");
		previousSession.setEffectiveRoles(Set.of("previous-role"));
		AuthenticationFailure failure = AuthenticationFailure.T.create();

		AttributeContexts.derivePeek()
				.set(UserSessionAspect.class, previousSession)
				.set(IsAuthorizedAspect.class, false)
				.set(RequestorSessionIdAspect.class, previousSession.getSessionId())
				.set(RequestorUserNameAspect.class, user.getName())
				.set(UserInfoAttribute.class, UserInfo.of(user.getName(), previousSession.getEffectiveRoles()))
				.set(LenientAuthenticationFailure.class, failure)
				.buildAnd().run(() -> {
					evaluateCheck();
					// Switching identity must not mutate the calling context.
					assertThat(AttributeContexts.peek().findOrNull(UserSessionAspect.class)).isSameAs(previousSession);
					assertThat(AttributeContexts.peek().findOrNull(RequestorUserNameAspect.class)).isEqualTo("previous-user");
					assertThat(AttributeContexts.peek().findOrNull(LenientAuthenticationFailure.class)).isSameAs(failure);
				});
	}

	private void evaluateCheck() {
		CheckSystemUserAttributes request = CheckSystemUserAttributes.T.create();
		request.setDomainId("internal");
		assertThat(request.eval(platformContract.serviceProcessing().systemEvaluator()).get()).isEqualTo(Boolean.TRUE);
	}

	/** Called by the wired processor, so assertions inspect the actual service-processing context. */
	public static Boolean checkAttributes(ServiceRequestContext context, UserSession expectedSession) {
		UserSession session = context.findOrNull(UserSessionAspect.class);
		assertThat(session).isSameAs(expectedSession);
		assertThat(session.getUser().getName()).isEqualTo("internal");
		assertThat(context.findOrNull(IsAuthorizedAspect.class)).isTrue();
		assertThat(context.findOrNull(RequestorSessionIdAspect.class)).isEqualTo(session.getSessionId()).isNotBlank();
		assertThat(context.findOrNull(RequestorUserNameAspect.class)).isEqualTo("internal");
		UserInfo info = context.findOrNull(UserInfoAttribute.class);
		assertThat(info).isNotNull();
		assertThat(info.userName()).isEqualTo("internal");
		assertThat(info.roles()).containsExactlyInAnyOrderElementsOf(session.getEffectiveRoles());
		assertThat(context.findOrNull(LenientAuthenticationFailure.class)).isNull();
		return Boolean.TRUE;
	}
}
