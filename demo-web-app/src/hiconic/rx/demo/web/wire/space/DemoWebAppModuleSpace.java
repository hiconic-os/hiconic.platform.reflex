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
package hiconic.rx.demo.web.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.demo.web.processing.AccessLogFilter;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebServerContract;
import jakarta.servlet.DispatcherType;

@Managed
public class DemoWebAppModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private WebServerContract webServer;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		webServer.addFilter("access-log", accessLogFilter());
		webServer.addFilterMapping("access-log", "/api/main/*", DispatcherType.REQUEST);
	}
	
	@Managed
	private AccessLogFilter accessLogFilter() {
		AccessLogFilter bean = new AccessLogFilter();
		return bean;
	}
}