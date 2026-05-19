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

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.time.TimeUnit;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;

import hiconic.rx.security.processor.usersession.base.UserSessionServiceTestBase;

/**
 * Tests on {@link UserSessionService} method used for {@link UserSession} creation.
 * 
 * <ul>
 * <li>{@link UserSessionService#createUserSession(User, Set, UserSessionType, TimeSpan, TimeSpan, Date, String, Map, String, boolean)}
 * </ul>
 */
public class JdbcUserSessionCreationTest extends UserSessionServiceTestBase {

	@Test
	public void testCreation_NullUser() throws Exception {
		Assertions.assertThat(testCreation(null, null, null, null, null, null, null, null).isUnsatisfiedBy(InvalidArgument.T)).isTrue();
	}

	@Test
	public void testCreation_EmptyUser() throws Exception {
		Assertions.assertThat(testCreation(UserConfig.emptyUser, null, null, null, null, null, null, null).isUnsatisfiedBy(InvalidArgument.T))
				.isTrue();
	}

	@Test
	public void testCreation_User() throws Exception {
		testCreation(UserConfig.user, null, null, null, null, null, null, null);
	}

	@Test
	public void testCreation_UserWithRoles() throws Exception {
		testCreation(UserConfig.userWithRoles, null, null, null, null, null, null, null);
	}

	@Test
	public void testCreation_UserWithGroups() throws Exception {
		testCreation(UserConfig.userWithGroups, null, null, null, null, null, null, null);
	}

	@Test
	public void testCreation_UserWithGroupsWithRoles() throws Exception {
		testCreation(UserConfig.userWithGroupsWithRoles, null, null, null, null, null, null, null);
	}

	@Test
	public void testCreation_UserWithRolesAndGroups() throws Exception {
		testCreation(UserConfig.userWithRolesAndGroups, null, null, null, null, null, null, null);
	}

	@Test
	public void testCreation_UserWithRolesAndGroupsWithRoles() throws Exception {
		testCreation(UserConfig.userWithRolesAndGroupsWithRoles, null, null, null, null, null, null, null);
	}

	@Test
	public void testCreation_InternalUserSessionType() throws Exception {
		testCreation(UserConfig.user, UserSessionType.internal, null, null, null, null, null, null);
	}

	@Test
	public void testCreation_TrustedUserSessionType() throws Exception {
		testCreation(UserConfig.user, UserSessionType.trusted, null, null, null, null, null, null);
	}

	@Test
	public void testCreation_MaxIdleTime() throws Exception {
		TimeSpan ts = tenMinuteTimeSpan();
		testCreation(UserConfig.user, null, ts, null, null, null, null, null);
	}

	@Test
	public void testCreation_MaxAge() throws Exception {
		TimeSpan ts = tenMinuteTimeSpan();
		testCreation(UserConfig.user, null, null, ts, null, null, null, null);
	}

	@Test
	public void testCreation_ExpiryDate() throws Exception {
		testCreation(UserConfig.user, null, null, null, createDate(1), null, null, null);
	}

	@Test
	public void testCreation_InternetAddress() throws Exception {
		testCreation(UserConfig.user, null, null, null, null, defaultInternetAddress, null, null);
	}

	@Test
	public void testCreation_Properties() throws Exception {
		testCreation(UserConfig.user, null, null, null, null, null, defaultProperties, null);
	}

	@Test
	public void testCreation_AcquirationKey() throws Exception {
		testCreation(UserConfig.user, null, null, null, null, null, null, "abc-opq-xyz");
	}

	private TimeSpan tenMinuteTimeSpan() {
		TimeSpan ts = TimeSpan.T.create();
		ts.setValue(10);
		ts.setUnit(TimeUnit.minute);
		return ts;
	}

	private Maybe<UserSession> testCreation(UserConfig userConfig, UserSessionType userSessionType, TimeSpan maxIdleTime, TimeSpan maxAge,
			Date expiryTime, String internetAddress, Map<String, String> properties, String acquirationKey) throws Exception {
		User user = getUser(userConfig);
		Set<String> expectedEffectiveRoles = getUserExpectedEffectiveRoles(userConfig);

		Maybe<UserSession> maybe = userSessionService.createUserSession(user, userSessionType, maxIdleTime, maxAge, expiryTime, internetAddress,
				properties, acquirationKey, false);

		if (maybe.isUnsatisfied())
			return maybe;

		if (maybe.isUnsatisfied())
			return maybe;

		UserSession userSession = maybe.get();

		assertUserSession(userSession, null, user, userSessionType, internetAddress, expectedEffectiveRoles, properties);

		UserSession foundUserSession = userSessionService.findUserSession(userSession.getSessionId()).get();

		assertUserSession(foundUserSession, null, user, userSessionType, internetAddress, expectedEffectiveRoles, properties);

		if (acquirationKey != null) {
			UserSession acquiredUserSession = userSessionService.findUserSessionByAcquirationKey(acquirationKey).get();

			assertUserSession(acquiredUserSession, userSession.getSessionId(), user, userSessionType, internetAddress, expectedEffectiveRoles,
					properties);
		}

		return maybe;
	}

}
