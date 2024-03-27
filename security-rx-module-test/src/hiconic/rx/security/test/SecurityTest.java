package hiconic.rx.security.test;

import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.model.processing.service.api.SessionIdAspect;
import com.braintribe.model.securityservice.Logout;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.security.model.test.RunSecured;
import hiconic.rx.test.common.AbstractRxTest;

public class SecurityTest extends AbstractRxTest {
	
	@Test
	public void testAuthenticationValidCredentials() {
		OpenUserSessionWithUserAndPassword openSession = OpenUserSessionWithUserAndPassword.T.create();
		openSession.setUser("reflector");
		openSession.setPassword("reflect");
		
		Maybe<? extends OpenUserSessionResponse> maybe = openSession.eval(evaluator).getReasoned();
		
		Assertions.assertThat(maybe.isSatisfied()).isTrue();
	}
	
	@Test
	public void testAuthenticationInvalidCredentials() {
		OpenUserSessionWithUserAndPassword openSession = OpenUserSessionWithUserAndPassword.T.create();
		openSession.setUser("reflector");
		openSession.setPassword("");
		
		Maybe<? extends OpenUserSessionResponse> maybe = openSession.eval(evaluator).getReasoned();
		
		Assertions.assertThat(maybe.isUnsatisfiedBy(InvalidCredentials.T)).isTrue();
	}
	
	@Test
	public void testRunSecuredUnauthenticated() {
		RunSecured runSecured = RunSecured.T.create();
		
		Maybe<?> maybe = runSecured.eval(evaluator).getReasoned();
		
		Assertions.assertThat(maybe.isUnsatisfiedBy(AuthenticationFailure.T)).isTrue();
	}
	
	@Test
	public void testRunSecuredAuthenticated() {
		RunSecured runSecured = RunSecured.T.create();

		Maybe<?> maybe = runAuthenticated(() -> runSecured.eval(evaluator).getReasoned());
		
		Assertions.assertThat(maybe.isSatisfied()).isTrue();
	}
	
	private <T> T runAuthenticated(Supplier<T> runner) {
		OpenUserSessionWithUserAndPassword openSession = OpenUserSessionWithUserAndPassword.T.create();
		openSession.setUser("reflector");
		openSession.setPassword("reflect");
		
		Maybe<? extends OpenUserSessionResponse> maybe = openSession.eval(evaluator).getReasoned();
		
		OpenUserSessionResponse response = maybe.get();
		
		try {
			return AttributeContexts.derivePeek().set(SessionIdAspect.class, response.getUserSession().getSessionId()).buildAnd().execute(runner);
		}
		finally {
			Logout logout = Logout.T.create();
			logout.setSessionId(response.getUserSession().getSessionId());
			logout.eval(evaluator).get();
		}
	}

}
