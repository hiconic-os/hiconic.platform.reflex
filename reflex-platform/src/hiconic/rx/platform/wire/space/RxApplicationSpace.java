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
package hiconic.rx.platform.wire.space;

import java.util.UUID;

import com.braintribe.model.service.api.InstanceId;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.log.RxLogManager;
import hiconic.rx.module.api.wire.RxApplicationContract;
import hiconic.rx.module.api.wire.RxConfigurationContract;
import hiconic.rx.platform.log.RxLogManagerImpl;
import hiconic.rx.platform.model.configuration.ReflexAppConfiguration;
import hiconic.rx.platform.processing.lifez.DeadlockChecker;
import hiconic.rx.platform.state.RxApplicationStateManagerImpl;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;

@Managed
public class RxApplicationSpace implements RxApplicationContract {

	@Import
	private RxPlatformConfigContract config;

	@Import
	private RxConfigurationContract configuration;

	@Override
	@Managed
	public RxLogManager logManager() {
		RxLogManagerImpl bean = new RxLogManagerImpl();
		return bean;
	}

	@Override
	@Managed
	public RxApplicationStateManagerImpl stateManager() {
		RxApplicationStateManagerImpl bean = new RxApplicationStateManagerImpl();
		bean.addLivenessChecker(new DeadlockChecker());
		return bean;
	}

	@Override
	public String applicationName() {
		return config.properties().applicationName();
	}

	@Override
	@Managed
	public String applicationId() {
		String bean = resolveApplicationId();
		return bean;
	}

	private String resolveApplicationId() {
		String result = appConfiguration().getApplicationId();
		if (result == null)
			result = appIdFromAppName();

		return result;
	}

	private String appIdFromAppName() {
		return applicationName().replace(" ", "-");
	}

	@Override
	@Managed
	public String nodeId() {
		String bean = resolveNodeId();
		return bean;
	}

	private String resolveNodeId() {
		String result = appConfiguration().getNodeId();
		if (result == null)
			result = UUID.randomUUID().toString();

		return result;
	}

	@Override
	@Managed
	public InstanceId instanceId() {
		InstanceId bean = InstanceId.T.create();
		bean.setApplicationId(applicationId());
		bean.setNodeId(nodeId());
		return bean;
	}

	private ReflexAppConfiguration appConfiguration() {
		return configuration.readConfig(ReflexAppConfiguration.T).get();
	}

}
