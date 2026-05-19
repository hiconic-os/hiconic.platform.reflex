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

import static com.braintribe.wire.api.util.Maps.entry;
import static com.braintribe.wire.api.util.Maps.map;

import com.braintribe.common.db.DbVendor;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.time.TimeUnit;
import com.braintribe.model.usersession.UserSessionType;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.security.processor.UserSessionIdProvider;
import hiconic.rx.security.processor.usersession.common.TestConfig;
import hiconic.rx.security.processor.usersession.wire.contract.TestContract;

@Managed
public abstract class BaseTestSpace implements TestContract {

	@Managed
	@Override
	public final TestConfig testConfig() {
		TestConfig bean = new TestConfig();
		bean.setGeneratingUserSessionAccesses(false);
		bean.setDbVendor(DbVendor.derby);
		return bean;
	}

	
	@Managed
	protected UserSessionIdProvider userSessionIdFactory() {
		// @formatter:off
		UserSessionIdProvider bean = new UserSessionIdProvider();
		bean.setTypePrefixes(
				map(
					entry(UserSessionType.internal, "i-"),
					entry(UserSessionType.trusted, "t-")
				)
			);
		return bean;
		// @formatter:on
	}

	@Managed
	protected TimeSpan defaultMaxIdleTime() {
		TimeSpan bean = TimeSpan.T.create();
		bean.setUnit(TimeUnit.hour);
		bean.setValue(24.0);
		return bean;
	}


}
