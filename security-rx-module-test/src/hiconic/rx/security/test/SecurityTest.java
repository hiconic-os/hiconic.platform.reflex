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
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.logging.Logger;
import com.braintribe.logging.ndc.mbean.NestedDiagnosticContext;
import com.braintribe.model.processing.service.api.SessionIdAspect;
import com.braintribe.model.processing.securityservice.api.attributes.OpenUserSessionEntryPointAttribute;
import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;
import com.braintribe.model.securityservice.Logout;
import com.braintribe.model.securityservice.OpenUserSession;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.securityservice.credentials.identification.UserNameIdentification;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.security.model.test.RunSecured;
import hiconic.rx.test.common.AbstractRxTest;

public class SecurityTest extends AbstractRxTest {
	private static final Logger logger = Logger.getLogger(SecurityTest.class);

	@Test
	public void defaultAdminRoleIsConfigured() {
		Assertions.assertThat(platformContract.auth().roleAuthorization().adminRoles()).containsExactly("admin");
	}
	
	@Test
	public void logTest() {
		NestedDiagnosticContext.pushContext("TestContext");
		NestedDiagnosticContext.put("Test", "Test-value");
		try {
			logger.info("Hello World", new RuntimeException("Test"));
		}
		finally {
			NestedDiagnosticContext.popContext();
		}
		logger.info("Without Context");
	}
	
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
	public void configuredEntryPointAuthorizesAndInducesRoles() {
		OpenUserSession request = openUserSession();
		request.setEntryPoint("admin-login");

		Maybe<? extends OpenUserSessionResponse> maybe = request.eval(platformContract.serviceProcessing().systemEvaluator()).getReasoned();

		Assertions.assertThat(maybe.isSatisfied()).isTrue();
		Assertions.assertThat(maybe.get().getReused()).isFalse();
		Assertions.assertThat(maybe.get().getUserSession().getEffectiveRoles()).contains("entry-point-user");
	}

	@Test
	public void configuredEntryPointRejectsMissingRole() {
		OpenUserSession request = openUserSession();
		request.setEntryPoint("customer-login");

		Maybe<? extends OpenUserSessionResponse> maybe = request.eval(platformContract.serviceProcessing().systemEvaluator()).getReasoned();

		Assertions.assertThat(maybe.isUnsatisfiedBy(Forbidden.T)).isTrue();
	}

	@Test
	public void contextualEntryPointFromAuthFilterIsApplied() {
		OpenUserSessionEntryPoint entryPoint = OpenUserSessionEntryPoint.T.create();
		entryPoint.setName("http-entry-point");
		entryPoint.getForbiddenRoles().add("admin");

		Maybe<? extends OpenUserSessionResponse> maybe = AttributeContexts.derivePeek() //
				.set(OpenUserSessionEntryPointAttribute.class, entryPoint) //
				.buildAnd().execute(() -> openUserSession().eval(evaluator).getReasoned());

		Assertions.assertThat(maybe.isUnsatisfiedBy(Forbidden.T)).isTrue();
	}

	@Test
	public void reusedSessionIsCheckedAgainstCurrentEntryPoint() {
		OpenUserSessionResponse initialResponse = openUserSession().eval(evaluator).getReasoned().get();

		ExistingSessionCredentials credentials = ExistingSessionCredentials.T.create();
		credentials.setExistingSessionId(initialResponse.getUserSession().getSessionId());
		credentials.setReuseSession(true);

		OpenUserSession reuseRequest = OpenUserSession.T.create();
		reuseRequest.setCredentials(credentials);
		reuseRequest.setEntryPoint("customer-login");

		Maybe<? extends OpenUserSessionResponse> maybe = reuseRequest.eval(platformContract.serviceProcessing().systemEvaluator()).getReasoned();

		Assertions.assertThat(maybe.isUnsatisfiedBy(Forbidden.T)).isTrue();
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

	private OpenUserSession openUserSession() {
		OpenUserSession request = OpenUserSession.T.create();
		UserPasswordCredentials credentials = UserPasswordCredentials.T.create();
		credentials.setPassword("reflect");
		UserNameIdentification identification = UserNameIdentification.T.create();
		identification.setUserName("reflector");
		credentials.setUserIdentification(identification);
		request.setCredentials(credentials);
		return request;
	}

}
