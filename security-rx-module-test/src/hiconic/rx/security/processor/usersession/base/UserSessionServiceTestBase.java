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
package hiconic.rx.security.processor.usersession.base;

import static com.braintribe.utils.lcd.CollectionTools2.asMap;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.braintribe.common.db.DbVendor;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.user.Group;
import com.braintribe.model.user.Identity;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.processing.test.db.derby.DerbyServerControl;
import com.braintribe.utils.FileTools;
import com.braintribe.wire.api.context.WireContext;

import hiconic.rx.security.processor.usersession.common.TestConfig;
import hiconic.rx.security.processor.usersession.wire.contract.TestContract;

public abstract class UserSessionServiceTestBase {

	protected static final Logger log = Logger.getLogger(UserSessionServiceTestBase.class);
	protected static final String defaultInternetAddress = "127.0.0.1";
	protected static final Map<String, String> defaultProperties = asMap("key-1", "value-1", "key-2", "value-2");
	protected static final String defaultServiceBeanId = "userSessionService";

	private static final Map<UserConfig, User> testUsers = new HashMap<>();
	private static final Map<UserConfig, Set<String>> testUsersExpectedEffectiveRoles = new HashMap<>();

	protected static TestConfig testConfig;
	protected static UserSessionService userSessionService;

	protected static WireContext<TestContract> context;
	private static DerbyServerControl derbyServerControl;

	// ============================= //
	// ======== LIFE CYCLE ========= //
	// ============================= //

	@BeforeClass
	public static void initialize() throws Exception {
		// clean db
		Path dbPath = Paths.get("res/db/dbtest");
		FileTools.deleteDirectoryRecursively(dbPath.toFile());

		context = TestContract.context();

		TestContract contract = context.contract();
		testConfig = contract.testConfig();

		if (DbVendor.derby.equals(testConfig.getDbVendor()))
			startDerbyDb();

		userSessionService = contract.userSessionService();
	}

	@AfterClass
	public static void destroy() throws Exception {
		if (context != null)
			context.shutdown();

		shutdownDerbyDb();
	}

	protected static void startDerbyDb() throws Exception {
		derbyServerControl = new DerbyServerControl();
		derbyServerControl.start();
	}

	protected static void shutdownDerbyDb() throws Exception {
		if (derbyServerControl != null) {
			derbyServerControl.stop();
			derbyServerControl = null;
		}
	}

	public enum UserConfig {
		emptyUser,
		user,
		userWithRoles,
		userWithGroups,
		userWithGroupsWithRoles,
		userWithRolesAndGroups,
		userWithRolesAndGroupsWithRoles,
	}

	static {
		// @formatter:off
		User.T.create();
		createUser(UserConfig.user								, false, 	false, 	false);
		createUser(UserConfig.userWithRoles						, true, 	false, 	false);
		createUser(UserConfig.userWithGroups					, false, 	true, 	false);
		createUser(UserConfig.userWithGroupsWithRoles			, false, 	false, 	true);
		createUser(UserConfig.userWithRolesAndGroups			, true, 	true, 	false);
		createUser(UserConfig.userWithRolesAndGroupsWithRoles	, true, 	true, 	true);
		// @formatter:on
	}

	protected static User getUser(UserConfig config) {
		return testUsers.get(config);
	}

	protected static Set<String> getUserExpectedEffectiveRoles(UserConfig config) {
		return testUsersExpectedEffectiveRoles.get(config);
	}

	protected static void assertUserSession(UserSession userSession, String givenSessionId, User givenUser, UserSessionType givenType,
			String givenInternetAddress, Set<String> expectedEffectiveRoles, Map<String, String> givenProperties) {

		Assert.assertNotNull(userSession);
		if (givenSessionId != null) {
			Assert.assertEquals(givenSessionId, userSession.getSessionId());
		} else {
			Assert.assertNotNull(userSession.getSessionId());
		}
		Assert.assertNotNull(userSession.getUser());
		Assert.assertEquals(givenUser.getName(), userSession.getUser().getName());
		Assert.assertEquals(givenUser.getFirstName(), userSession.getUser().getFirstName());
		Assert.assertEquals(givenUser.getLastName(), userSession.getUser().getLastName());
		Assert.assertEquals(givenUser.getEmail(), userSession.getUser().getEmail());

		Assert.assertNotNull(userSession.getCreationDate());
		Assert.assertNotNull(userSession.getLastAccessedDate());
		Assert.assertNotNull(userSession.getType());
		Assert.assertNotNull(userSession.getUser());
		Assert.assertNotNull(userSession.getEffectiveRoles());
		if (expectedEffectiveRoles != null) {
			Assert.assertEquals(expectedEffectiveRoles, userSession.getEffectiveRoles());
		} else {
			Assert.assertTrue(userSession.getEffectiveRoles() == null || userSession.getEffectiveRoles().isEmpty());
		}
		Assert.assertEquals(givenInternetAddress, userSession.getCreationInternetAddress());
		if (givenType != null) {
			Assert.assertEquals(givenType, userSession.getType());
		} else {
			Assert.assertEquals(UserSessionType.normal, userSession.getType());
		}
		if (givenProperties != null) {
			Assert.assertEquals(givenProperties, userSession.getProperties());
		} else {
			Assert.assertTrue(userSession.getProperties() == null || userSession.getProperties().isEmpty());
		}

		// still need to test expiry dates
	}

	private static final User createUser(UserConfig config, boolean withRoles, boolean withGroups, boolean withGroupsWithRoles) {

		String id = UUID.randomUUID().toString();

		User user = User.T.create();
		user.setId(id);
		user.setName("name-" + id);
		user.setEmail("email-" + id);
		user.setFirstName("firstName-" + id);
		user.setLastName("lastName-" + id);

		if (withRoles) {
			addRoles(user, 3);
		}

		if (withGroups) {
			addGroups(user, 3, false);
		}

		if (withGroupsWithRoles) {
			addGroups(user, 3, true);
		}

		testUsers.put(config, user);
		testUsersExpectedEffectiveRoles.put(config, collectUserEffectiveRoles(user));

		return user;

	}

	private static final void addRoles(Identity identity, int t) {
		if (identity.getRoles() == null) {
			identity.setRoles(new HashSet<Role>());
		}

		for (int i = 0; i < t; i++) {
			String id = UUID.randomUUID().toString();
			Role role = Role.T.create();
			role.setId(id);
			role.setName("name-" + id);

			identity.getRoles().add(role);
		}

		// add a null Role which shouldn't cause errors
		identity.getRoles().add(null);

		// add a Role with null name which shouldn't cause errors
		Role role = Role.T.create();
		role.setId(UUID.randomUUID().toString());
		identity.getRoles().add(role);

	}

	private static final void addGroups(User user, int t, boolean withRoles) {
		if (user.getGroups() == null) {
			user.setGroups(new HashSet<Group>());
		}

		for (int i = 0; i < t; i++) {
			String id = UUID.randomUUID().toString();
			Group group = Group.T.create();
			group.setId(id);
			group.setName("name-" + id);

			if (withRoles) {
				addRoles(group, 4);
			}

			user.getGroups().add(group);
		}

		// add a null Group which shouldn't cause errors
		user.getGroups().add(null);

		// add a Group with null name which shouldn't cause errors
		Group group = Group.T.create();
		group.setId(UUID.randomUUID().toString());
		user.getGroups().add(group);

	}

	private static Set<String> collectUserEffectiveRoles(User user) {
		Set<String> effectiveRoles = new HashSet<String>();

		if (user.getRoles() != null) {
			for (Role role : user.getRoles()) {
				if (role != null && role.getName() != null) {
					effectiveRoles.add(role.getName());
				}

			}
		}
		if (user.getGroups() != null) {
			for (Group group : user.getGroups()) {
				if (group != null) {
					if (group.getRoles() != null) {
						for (Role role : group.getRoles()) {
							if (role != null && role.getName() != null) {
								effectiveRoles.add(role.getName());
							}
						}
					}
					if (group.getName() != null) {
						// group as dynamic role
						effectiveRoles.add("$group-" + group.getName());
					}
				}
			}
		}

		// adding remaining "$user" and "$all" dynamic roles
		effectiveRoles.add("$user-" + user.getName());
		effectiveRoles.add("$all");

		return effectiveRoles;
	}

	protected static Set<String> asUserEffectiveRoles(String userName, String... roles) {
		Set<String> effectiveRoles = new HashSet<String>();

		if (roles != null && roles.length > 0) {
			effectiveRoles.addAll(Arrays.asList(roles));
		}
		effectiveRoles.add("$user-" + userName);
		effectiveRoles.add("$all");

		return effectiveRoles;
	}

	protected Date createDate(int addYears) {
		Calendar c = Calendar.getInstance();
		Date now = new Date();
		c.setTime(now);
		c.add(Calendar.YEAR, addYears);
		return c.getTime();
	}

}
