// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.gm.model.security.reason.SessionNotFound;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;

import hiconic.rx.security.processor.usersession.base.UserSessionServiceTestBase;

/**
 * Tests on {@link UserSessionService} method(s) used for {@link UserSession} retrieval.
 * 
 * <ul>
 * <li>{@link UserSessionService#findUserSession(String)}
 * <li>{@link UserSessionService#findUserSessionByAcquirationKey(String)}
 * </ul>
 */
public class JdbcUserSessionRetrievalTest extends UserSessionServiceTestBase {

	@Test
	public void testFindUserSession_NullSessionId() throws Exception {
		Assert.assertTrue(userSessionService.findUserSession(null).isUnsatisfiedBy(SessionNotFound.T));
	}

	@Test
	public void testFindUserSession() throws Exception {
		User user1 = getUser(UserConfig.user);
		User user2 = getUser(UserConfig.userWithRoles);
		Set<String> expectedEffectiveRoles1 = getUserExpectedEffectiveRoles(UserConfig.user);
		Set<String> expectedEffectiveRoles2 = getUserExpectedEffectiveRoles(UserConfig.userWithRoles);

		UserSession userSession1 = userSessionService
				.createUserSession(user1, UserSessionType.normal, null, null, null, defaultInternetAddress, defaultProperties, null, false).get();
		UserSession userSession2 = userSessionService
				.createUserSession(user2, UserSessionType.internal, null, null, null, defaultInternetAddress, defaultProperties, null, false).get();

		UserSession foundUserSession1 = userSessionService.findUserSession(userSession1.getSessionId()).get();
		UserSession foundUserSession2 = userSessionService.findUserSession(userSession2.getSessionId()).get();

		assertUserSession(foundUserSession1, null, user1, UserSessionType.normal, defaultInternetAddress, expectedEffectiveRoles1, defaultProperties);
		assertUserSession(foundUserSession2, null, user2, UserSessionType.internal, defaultInternetAddress, expectedEffectiveRoles2,
				defaultProperties);
	}

}
