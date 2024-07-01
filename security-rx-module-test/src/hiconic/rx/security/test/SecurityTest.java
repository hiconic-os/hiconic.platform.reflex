// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
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
