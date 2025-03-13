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
package hiconic.platform.reflex.web_rpc.wire.space;

import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import dev.hiconic.servlet.webrpc.server.GmWebRpcServer;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebServerContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class WebRpcServerRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private WebServerContract webServer;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		webServer.addServlet("web-rpc", "/rpc/*", webRpcServer());
	}

	@Managed
	private GmWebRpcServer webRpcServer() {
		GmWebRpcServer bean = new GmWebRpcServer();
		
		bean.setEvaluator(platform.evaluator());
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setStreamPipeFactory(StreamPipes.simpleFactory());
		
		return bean;
	}
}