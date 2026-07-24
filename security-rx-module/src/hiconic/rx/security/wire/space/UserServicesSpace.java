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
package hiconic.rx.security.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;
import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.time.TimeUnit;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.api.UserService;
import hiconic.rx.security.model.configuration.SecurityConfiguration;
import hiconic.rx.security.model.configuration.UserServiceConfiguration;
import hiconic.rx.security.model.configuration.UserProvisioningConfiguration;
import hiconic.rx.security.model.configuration.UsersConfiguration;
import hiconic.rx.security.processor.DelegatingUserService;
import hiconic.rx.security.processor.JdbcUserSessionService;
import hiconic.rx.security.processor.StandardUserSessionService;
import hiconic.rx.security.processor.StaticUserService;
import hiconic.rx.security.processor.UserSessionIdProvider;

@Managed
public class UserServicesSpace implements WireSpace {
	@Import
	private RxPlatformContract platform;

	@Import
	private DatabaseContract database;

	@Managed
	private SecurityConfiguration configuration() {
		return getOrTunnel(platform.configuration().readConfig(SecurityConfiguration.T));
	}

	@Managed
	private UsersConfiguration usersConfiguration() {
		return getOrTunnel(platform.configuration().readConfig(UsersConfiguration.T));
	}

	@Managed
	private UserServiceConfiguration userServiceConfiguration() {
		return getOrTunnel(platform.configuration().readConfig(UserServiceConfiguration.T));
	}

	@Managed
	private UserProvisioningConfiguration userProvisioningConfiguration() {
		return getOrTunnel(platform.configuration().readConfig(UserProvisioningConfiguration.T));
	}

	public void provisionConfiguredUsers() {
		UserService userService = userService();
		if (UserServiceConfiguration.DEFAULT_USER_SERVICE_ID.equals(userService.userServiceId()))
			return;

		for (User user : userProvisioningConfiguration().getUsers()) {
			Reason error = userService.ensureUser(user);
			if (error != null)
				throw new ReasonException(error);
		}
	}

	public UserSessionService userSessionService() {
		// TODO why is the fallback not the main DB?
		DataSource dataSource = database.findDataSource(configuration().getUserSessionDb());

		if (dataSource != null)
			return jdbcUserSessionService(dataSource);
		else
			return standardUserSessionService();
	}

	public void registerUserService(UserService userService) {
		delegatingUserService().registerUserService(userService);
	}

	public UserService userService() {
		return delegatingUserService();
	}

	@Managed
	private DelegatingUserService delegatingUserService() {
		DelegatingUserService bean = new DelegatingUserService();
		bean.setDelegateUserServiceId(userServiceConfiguration().getUserServiceId());
		bean.registerUserService(staticUserService());
		return bean;
	}

	@Managed
	private StaticUserService staticUserService() {
		StaticUserService bean = new StaticUserService();
		bean.setUsers(configuredUsersAndSystemUser());
		return bean;
	}

	private List<User> configuredUsersAndSystemUser() {
		ArrayList<User> result = newList(usersConfiguration().getUsers());
		result.add(platform.auth().systemUser());

		return result;
	}

	@Managed
	private StandardUserSessionService standardUserSessionService() {
		StandardUserSessionService bean = new StandardUserSessionService();
		bean.setSessionIdProvider(userSessionIdFactory());
		bean.setNodeId(platform.application().nodeId());
		bean.setDefaultUserSessionMaxIdleTime(TimeSpan.create(24, TimeUnit.hour));
		return bean;
	}

	@Managed
	private JdbcUserSessionService jdbcUserSessionService(DataSource dataSource) {
		JdbcUserSessionService bean = new JdbcUserSessionService();
		bean.setDataSource(dataSource);
		bean.setSessionIdProvider(userSessionIdFactory());
		bean.setNodeId(platform.application().nodeId());
		bean.setDefaultUserSessionMaxIdleTime(TimeSpan.create(24, TimeUnit.hour));
		bean.setSystemUserSession(platform.auth().systemUserSession());
		return bean;
	}

	@Managed
	public UserSessionIdProvider userSessionIdFactory() {
		UserSessionIdProvider bean = new UserSessionIdProvider();
		bean.setTypePrefixes(Map.of( //
				UserSessionType.internal, "i-", //
				UserSessionType.trusted, "t-" //
		));
		return bean;
	}
}
