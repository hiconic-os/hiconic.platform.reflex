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
package hiconic.rx.casting.messaging.wire.space;

import java.util.Collections;

import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.casting.messaging.processing.MulticastRxProcessor;
import hiconic.rx.messaging.api.MessagingContract;
import hiconic.rx.messaging.api.MessagingDestinationsContract;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.topology.api.TopologyContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
@SuppressWarnings("deprecation")
public class CastingViaMessagingRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private MessagingContract messaging;

	@Import
	private MessagingDestinationsContract messagingDestinations;

	@Import
	private TopologyContract topology;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration systemSd = configurations.system();

		systemSd.bindRequest(MulticastRequest.T, this::multicastProcessor);
	}

	@Managed
	private MulticastRxProcessor multicastProcessor() {
		MulticastRxProcessor bean = new MulticastRxProcessor();
		bean.setMessagingSessionProvider(messaging.sessionProvider());
		bean.setRequestTopicName(messagingDestinations.multicastRequestTopicName());
		bean.setResponseTopicName(messagingDestinations.multicastResponseTopicName());
		bean.setSenderId(platform.instanceId());
		bean.setLiveInstances(topology.liveInstances());
		// TODO configure clientMetaDataProvider
		bean.setMetaDataProvider(Collections::emptyMap);
		// bean.setMetaDataProvider(rpc.clientMetaDataProvider()); // RpcSpace.clientMetaDataProvider()

		return bean;
	}

}