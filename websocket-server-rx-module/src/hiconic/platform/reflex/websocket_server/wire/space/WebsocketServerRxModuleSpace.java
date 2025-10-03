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
package hiconic.platform.reflex.websocket_server.wire.space;

import com.braintribe.model.service.api.PushRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.websocket_server.processing.PushChannelLifecycleHub;
import hiconic.platform.reflex.websocket_server.processing.WsServer;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.push.api.PushContract;
import hiconic.rx.web.server.api.WebServerContract;

@Managed
public class WebsocketServerRxModuleSpace implements RxModuleContract, PushContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private WebServerContract webServer;
	
	@Override
	public void onDeploy() {
		webServer.addEndpoint("/ws", server());
		webServer.addEndpoint("/websocket", server());
	}
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		// TODO: use PushRequest, MulticastRequest and InternalPushRequest to enable PushRequest in distributed setups
		configuration.bindRequest(PushRequest.T, this::server);
	}

	@Managed
	private WsServer server() {
		WsServer bean = new WsServer();
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setProcessingInstanceId(platform.instanceId());
		bean.setEvaluator(platform.systemEvaluator());
		bean.setPushChannelLifecycleHub(channelLifecyclePublisher());
		return bean;
	}
	
	@Managed
	@Override
	public PushChannelLifecycleHub channelLifecyclePublisher() {
		PushChannelLifecycleHub bean = new PushChannelLifecycleHub();
		return bean;
	}
}