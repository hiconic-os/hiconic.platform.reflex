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
package hiconic.rx.security.processor.usersession.wire.space;

import java.util.UUID;

import com.braintribe.common.db.wire.contract.DbTestDataSourcesContract;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.security.processor.JdbcUserSessionService;

@Managed
public class DbBasedTestSpace extends BaseTestSpace {

	@Import
	private DbTestDataSourcesContract dbTestDataSources;

	@Override
	public UserSessionService userSessionService() {
		return jdbcService();
	}

	@Managed
	public JdbcUserSessionService jdbcService() {
		JdbcUserSessionService bean = new JdbcUserSessionService();
		bean.setDataSource(dbTestDataSources.dataSource(testConfig().getDbVendor()));
		bean.setSessionIdProvider(userSessionIdFactory());
		bean.setDefaultUserSessionMaxIdleTime(defaultMaxIdleTime());
		bean.setSystemUserSession(systemUserSession());

		return bean;
	}

	private UserSession systemUserSession() {
		UserSession bean = UserSession.T.create();
		bean.setSessionId(UUID.randomUUID().toString());
		return bean;
	}

}
