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

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.websocket_server.processing.WsServer;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.push.api.PushContract;
import hiconic.rx.web.server.api.WebServerContract;

@Managed
public class WebsocketServerRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private WebServerContract webServer;

	@Import
	private PushContract push;
	
	@Override
	public void onDeploy() {
		webServer.addEndpoint(webServer.pushWebSocketEndpointPath(), server());
		push.addHandler(server());
	}

	@Managed
	private WsServer server() {
		WsServer bean = new WsServer();
		bean.setMarshallerRegistry(platform.marshalling().marshallers());
		bean.setProcessingInstanceId(platform.application().instanceId());
		bean.setEvaluator(platform.serviceProcessing().systemEvaluator());
		bean.setPushContract(push);
		return bean;
	}
}
