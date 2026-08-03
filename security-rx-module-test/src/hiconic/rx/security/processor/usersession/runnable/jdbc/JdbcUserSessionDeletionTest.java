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
package hiconic.rx.security.processor.usersession.runnable.jdbc;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.security.reason.SessionNotFound;
import com.braintribe.model.processing.securityservice.api.DeletedSessionInfo;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;

import hiconic.rx.security.processor.usersession.base.UserSessionServiceTestBase;
import hiconic.rx.security.api.UserSessionInvalidation;

/**
 * Tests on {@link UserSessionService} method(s) used for {@link UserSession} deletion.
 * 
 * <ul>
 * <li>{@link UserSessionService#deleteUserSession(String)}
 * </ul>
 */
public class JdbcUserSessionDeletionTest extends UserSessionServiceTestBase {

	@Test
	public void testDeletion_NullSessionId() throws Exception {
		Maybe<DeletedSessionInfo> session = userSessionService.deleteUserSession(null);
		Assert.assertTrue(session.isUnsatisfiedBy(SessionNotFound.T));
	}

	@Test
	public void testDeletion() throws Exception {
		User user1 = getUser(UserConfig.user);
		User user2 = getUser(UserConfig.userWithRoles);
		User user3 = getUser(UserConfig.userWithGroups);

		UserSession userSession1 = userSessionService.createUserSession(user1, null, null, null, null, null, null, null, false).get();
		UserSession userSession2 = userSessionService.createUserSession(user2, null, null, null, null, null, null, null, false).get();
		UserSession userSession3 = userSessionService.createUserSession(user3, null, null, null, null, null, null, null, false).get();

		UserSession deletedUserSession1 = userSessionService.deleteUserSession(userSession1.getSessionId()).get().userSession();

		Assert.assertNotNull(deletedUserSession1);
		Assert.assertEquals(deletedUserSession1.getSessionId(), userSession1.getSessionId());

		Assert.assertTrue(userSessionService.findUserSession(userSession1.getSessionId()).isUnsatisfiedBy(SessionNotFound.T));
		Assert.assertNotNull(userSessionService.findUserSession(userSession2.getSessionId()).get());
		Assert.assertNotNull(userSessionService.findUserSession(userSession3.getSessionId()).get());

		UserSession deletedUserSession2 = userSessionService.deleteUserSession(userSession2.getSessionId()).get().userSession();

		Assert.assertNotNull(deletedUserSession2);
		Assert.assertEquals(deletedUserSession2.getSessionId(), userSession2.getSessionId());

		Assertions.assertThat(userSessionService.findUserSession(userSession2.getSessionId()).isUnsatisfiedBy(SessionNotFound.T)).isTrue();
		Assertions.assertThat(userSessionService.findUserSession(userSession3.getSessionId()).isSatisfied()).isTrue();

		UserSession deletedUserSession3 = userSessionService.deleteUserSession(userSession3.getSessionId()).get().userSession();

		Assert.assertNotNull(deletedUserSession3);
		Assert.assertEquals(deletedUserSession3.getSessionId(), userSession3.getSessionId());

		Assert.assertTrue(userSessionService.findUserSession(userSession3.getSessionId()).isUnsatisfiedBy(SessionNotFound.T));
	}

	@Test
	public void invalidatesAllSessionsOfOneUser() {
		User user1 = getUser(UserConfig.user);
		User user2 = getUser(UserConfig.userWithRoles);
		UserSession userSession1a = userSessionService.createUserSession(user1, null, null, null, null, null, null, null, false).get();
		UserSession userSession1b = userSessionService.createUserSession(user1, null, null, null, null, null, null, null, false).get();
		UserSession userSession2 = userSessionService.createUserSession(user2, null, null, null, null, null, null, null, false).get();

		int invalidated = ((UserSessionInvalidation) userSessionService).invalidateUserSessions(user1.getName());

		Assertions.assertThat(invalidated).isEqualTo(2);
		Assertions.assertThat(userSessionService.findUserSession(userSession1a.getSessionId()).isUnsatisfiedBy(SessionNotFound.T)).isTrue();
		Assertions.assertThat(userSessionService.findUserSession(userSession1b.getSessionId()).isUnsatisfiedBy(SessionNotFound.T)).isTrue();
		Assertions.assertThat(userSessionService.findUserSession(userSession2.getSessionId()).isSatisfied()).isTrue();
	}

}
