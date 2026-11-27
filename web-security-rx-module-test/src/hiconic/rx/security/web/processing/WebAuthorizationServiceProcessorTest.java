package hiconic.rx.security.web.processing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.MissingCredentials;
import com.braintribe.gm.model.security.reason.MissingSession;
import com.braintribe.gm.model.security.reason.SessionExpired;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.securityservice.api.attributes.LenientAuthenticationFailure;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.service.commons.StandardServiceRequestContextBuilder;
import com.braintribe.model.securityservice.web.GetWebAuthorization;
import com.braintribe.model.securityservice.web.WebAuthorization;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;

public class WebAuthorizationServiceProcessorTest {

	private final WebAuthorizationServiceProcessor processor = new WebAuthorizationServiceProcessor(null);

	@Test
	public void missingCredentialsBecomeMissingSession() {
		AuthenticationFailure failure = Reasons.build(MissingCredentials.T).text("No credentials found in http request").toReason();

		Maybe<? extends WebAuthorization> result = currentAuthorization(context(null, failure));

		assertThat(result.isUnsatisfiedBy(MissingSession.T)).isTrue();
	}

	@Test
	public void concreteAuthenticationFailureIsPropagated() {
		AuthenticationFailure failure = Reasons.build(SessionExpired.T).text("User session has expired.").toReason();

		Maybe<? extends WebAuthorization> result = currentAuthorization(context(null, failure));

		assertThat((Reason) result.whyUnsatisfied()).isSameAs(failure);
	}

	@Test
	public void authenticatedSessionProducesAuthorization() {
		User user = User.T.create();
		user.setName("alice");
		user.setPassword("secret");
		UserSession session = UserSession.T.create();
		session.setUser(user);

		Maybe<? extends WebAuthorization> result = currentAuthorization(context(session, null));

		assertThat(result.isSatisfied()).isTrue();
		assertThat(result.get().getUser().getName()).isEqualTo("alice");
		assertThat(result.get().getUser().getPassword()).isNull();
	}

	private Maybe<? extends WebAuthorization> currentAuthorization(ServiceRequestContext context) {
		return processor.processReasoned(context, GetWebAuthorization.T.create());
	}

	@SuppressWarnings("unchecked")
	private ServiceRequestContext context(UserSession session, AuthenticationFailure failure) {
		StandardServiceRequestContextBuilder builder = new StandardServiceRequestContextBuilder((Evaluator<ServiceRequest>) null);
		if (session != null)
			builder.set(UserSessionAspect.class, session);
		if (failure != null)
			builder.set(LenientAuthenticationFailure.class, failure);
		return builder.build();
	}
}
