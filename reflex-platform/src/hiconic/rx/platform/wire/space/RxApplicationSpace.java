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
import com.braintribe.provider.Box;
import com.braintribe.provider.Holder;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.log.RxLogManager;
import hiconic.rx.module.api.wire.RxApplicationContract;
import hiconic.rx.platform.log.RxLogManagerImpl;
import hiconic.rx.platform.model.configuration.ReflexAppConfiguration;
import hiconic.rx.platform.processing.cluster.SingleInstanceLiveInstances;
import hiconic.rx.platform.processing.lifez.DeadlockChecker;
import hiconic.rx.platform.state.RxApplicationStateManagerImpl;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;
import hiconic.rx.topology.api.LiveInstances;

@Managed
public class RxApplicationSpace implements RxApplicationContract {

	@Import
	private RxPlatformConfigContract config;

	@Import
	private RxConfigurationSpace configuration;

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
	
	@Managed
	public Box<LiveInstances> liveInstancesBox() {
		return Box.of(singleInstanceLiveInstances());
	}
	
	private SingleInstanceLiveInstances singleInstanceLiveInstances() {
		SingleInstanceLiveInstances bean = new SingleInstanceLiveInstances();
		bean.setInstanceId(instanceId());
		return bean;
	}
	
	@Override
	public LiveInstances liveInstances() {
		return liveInstancesBox().value;
	}

	private ReflexAppConfiguration appConfiguration() {
		return configuration.readConfig(ReflexAppConfiguration.T).get();
	}

}
