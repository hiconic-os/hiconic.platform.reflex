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

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.time.TimeUnit;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;

import hiconic.rx.security.processor.usersession.base.UserSessionServiceTestBase;

/**
 * Tests on {@link UserSessionService} method(s) used for {@link UserSession} touch.
 * 
 * <ul>
 * <li>{@link UserSessionService#touchUserSession(String, Date, Date)}
 * </ul>
 */
public class JdbcUserSessionTouchingTest extends UserSessionServiceTestBase {

	@Test
	public void testTouchUserSession() throws Exception {
		User user1 = getUser(UserConfig.user);
		User user2 = getUser(UserConfig.userWithRoles);

		TimeSpan maxIdleTime = TimeSpan.T.create();
		maxIdleTime.setValue(1);
		maxIdleTime.setUnit(TimeUnit.day);

		UserSession userSession1 = userSessionService
				.createUserSession(user1, UserSessionType.normal, maxIdleTime, null, null, defaultInternetAddress, defaultProperties, null, false)
				.get();
		UserSession userSession2 = userSessionService
				.createUserSession(user2, UserSessionType.internal, maxIdleTime, null, null, defaultInternetAddress, defaultProperties, null, false)
				.get();

		Thread.sleep(10);

		Date lastAccessDate = new Date();
		Date expiryDate = new Date(lastAccessDate.getTime() + maxIdleTime.toLongMillies());

		userSessionService.touchUserSession(userSession1.getSessionId(), lastAccessDate, expiryDate);
		userSessionService.touchUserSession(userSession2.getSessionId(), lastAccessDate, expiryDate);

		UserSession foundUserSession1 = userSessionService.findUserSession(userSession1.getSessionId()).get();
		UserSession foundUserSession2 = userSessionService.findUserSession(userSession2.getSessionId()).get();

		Assert.assertTrue(userSession1.getLastAccessedDate().before(foundUserSession1.getLastAccessedDate()));
		Assert.assertTrue(userSession1.getExpiryDate().before(foundUserSession1.getExpiryDate()));
		Assert.assertTrue(userSession2.getLastAccessedDate().before(foundUserSession2.getLastAccessedDate()));
		Assert.assertTrue(userSession2.getExpiryDate().before(foundUserSession2.getExpiryDate()));
	}

}
