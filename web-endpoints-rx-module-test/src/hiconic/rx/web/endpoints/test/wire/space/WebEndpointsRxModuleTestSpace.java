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
package hiconic.rx.web.endpoints.test.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.demo.model.api.ReverseText;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.web.ddra.endpoints.api.WebApiServerContract;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;

@Managed
public class WebEndpointsRxModuleTestSpace implements RxModuleContract {

	@Import
	private WebApiServerContract webApiServer;

	@Override
	public void onDeploy() {
		webApiServer.mappingRegistry().mapping("/lenient", HttpRequestMethod.POST, ReverseText.T) //
				.decodingLenience(true) //
				.register();
		webApiServer.mappingRegistry().mapping("/strict", HttpRequestMethod.POST, ReverseText.T) //
				.decodingLenience(false) //
				.register();
	}
}
