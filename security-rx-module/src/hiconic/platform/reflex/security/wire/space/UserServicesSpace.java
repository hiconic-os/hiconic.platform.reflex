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
package hiconic.platform.reflex.security.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.Map;

import javax.sql.DataSource;

import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.time.TimeUnit;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.security.processor.JdbcUserSessionService;
import hiconic.platform.reflex.security.processor.StandardUserService;
import hiconic.platform.reflex.security.processor.StandardUserSessionService;
import hiconic.platform.reflex.security.processor.UserSessionIdProvider;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.model.configuration.SecurityConfiguration;
import hiconic.rx.security.model.configuration.UsersConfiguration;

@Managed
public class UserServicesSpace implements RxModuleContract {
	@Import
	private RxPlatformContract platform;
	
	@Import
	private DatabaseContract database;

	@Managed
	private SecurityConfiguration configuration() {
		return getOrTunnel(platform.readConfig(SecurityConfiguration.T));
	}
	
	@Managed
	private UsersConfiguration usersConfiguration() {
		return getOrTunnel(platform.readConfig(UsersConfiguration.T));
	}

	public UserSessionService userSessionService() {
		// TODO why is the fallback not the main DB?
		DataSource dataSource = database.findDataSource(configuration().getUserSessionDb());
		
		if (dataSource != null)
			return jdbcUserSessionService(dataSource);
		else
			return standardUserSessionService();
	}
	
	@Managed
	public StandardUserService standardUserService() {
		StandardUserService bean = new StandardUserService();
		bean.setUsers(usersConfiguration().getUsers());
		return bean;
	}
	
	@Managed 
	private StandardUserSessionService standardUserSessionService() {
		StandardUserSessionService bean = new StandardUserSessionService();
		bean.setSessionIdProvider(userSessionIdFactory());
		bean.setNodeId(platform.nodeId());
		bean.setDefaultUserSessionMaxIdleTime(TimeSpan.create(24, TimeUnit.hour));
		return bean;
	}
	
	@Managed
	private JdbcUserSessionService jdbcUserSessionService(DataSource dataSource) {
		JdbcUserSessionService bean = new JdbcUserSessionService();
		bean.setDataSource(dataSource);
		bean.setSessionIdProvider(userSessionIdFactory());
		bean.setNodeId(platform.nodeId());
		bean.setDefaultUserSessionMaxIdleTime(TimeSpan.create(24, TimeUnit.hour));
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