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
package hiconic.rx.topology.module.wire.space;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.braintribe.execution.ExtendedScheduledThreadPoolExecutor;
import com.braintribe.execution.virtual.CountingVirtualThreadFactory;
import com.braintribe.logging.Logger;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.messaging.api.MessagingContract;
import hiconic.rx.messaging.api.MessagingDestinationsContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.topology.api.TopologyContract;
import hiconic.rx.topology.module.processing.RxHeartbeatManager;
import hiconic.rx.topology.module.processing.RxLiveInstances;

@Managed
public class TopologyViaMessagingModuleSpace implements RxModuleContract, TopologyContract {

	private static final Logger logger = Logger.getLogger(TopologyViaMessagingModuleSpace.class);

	@Import
	private RxPlatformContract platform;

	@Import
	private MessagingContract messaging;

	@Import
	@SuppressWarnings("deprecation")
	private MessagingDestinationsContract messagingDestinations;

	@Override
	public void onApplicationReady() {
		logger.info(() -> "Starting RxHeartbeatManager signalling instance id: " + platform.instanceId());
		heartbeatManager().startHeartbeatBroadcasting();
	}

	@Override
	@Managed
	public RxLiveInstances liveInstances() {
		RxLiveInstances bean = new RxLiveInstances();
		bean.setCurrentInstanceId(platform.instanceId());
		bean.setEnabled(consumeHeartbeats());
		bean.setAliveAge(30000); // 30 seconds
		bean.setMaxHeartbeatAge(30000); // 30 seconds
		bean.setCleanupInterval(600000); // 10 minutes
		return bean;
	}

	@Managed
	@SuppressWarnings("deprecation")
	public RxHeartbeatManager heartbeatManager() {
		RxHeartbeatManager bean = new RxHeartbeatManager();
		bean.setCurrentInstanceId(platform.instanceId());
		bean.setConsumptionEnabled(consumeHeartbeats());
		bean.setHeartbeatConsumer(liveInstances());
		bean.setBroadcastingEnabled(true);
		bean.setBroadcastingService(heartbeatBroadcastingService());
		bean.setBroadcastingInterval(10L);
		bean.setBroadcastingIntervalUnit(TimeUnit.SECONDS);
		bean.setMessagingSessionProvider(messaging.sessionProvider());
		bean.setTopicName(messagingDestinations.heartbeatTopicName());
		bean.setAutoHeartbeatBroadcastingStart(false);
		return bean;
	}

	private boolean consumeHeartbeats() {
		return true; // Whether the platform should track live instances. Could be later configurable.
	}

	@Managed
	private ScheduledExecutorService heartbeatBroadcastingService() {
		ExtendedScheduledThreadPoolExecutor bean = new ExtendedScheduledThreadPoolExecutor(1,
				new CountingVirtualThreadFactory("reflex.heartbeat.sender[" + platform.instanceId() + "]-"));
		bean.setDescription("Rx Heartbeat Sender");
		return bean;
	}

}
