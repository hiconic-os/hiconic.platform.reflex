// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.model.browser.wire.space;

import java.util.Set;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.model.browser.processing.ModelBrowserServlet;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.web.api.AuthFilters;
import hiconic.rx.web.server.api.WebAppNavigationEntry;
import hiconic.rx.web.server.api.WebServerContract;
import jakarta.servlet.DispatcherType;

@Managed
public class ModelBrowserRxModuleSpace implements RxModuleContract {

	public static final String PATH = "model-browser";

	@Import
	private RxPlatformContract platform;

	@Import
	private WebServerContract webServer;

	@Override
	public void onDeploy() {
		webServer.addServlet("model-browser", PATH + "/*", modelBrowserServlet());
		/*
		 * The navigation role set only controls discovery on the landing page. The
		 * strict admin UI filter is the actual HTTP security boundary and derives
		 * its roles from the central RX security configuration.
		 */
		webServer.addFilterMapping(AuthFilters.strictAdminUiAuthFilter, "/" + PATH + "/*", DispatcherType.REQUEST);

		Set<String> adminRoles = platform.auth().roleAuthorization().adminRoles();
		webServer.addWebAppNavigation(new WebAppNavigationEntry(PATH, "Model Browser",
				"Inspect configured models, their types, dependencies and properties.", adminRoles, 110));
	}

	@Managed
	private ModelBrowserServlet modelBrowserServlet() {
		ModelBrowserServlet bean = new ModelBrowserServlet();
		bean.setConfiguredModels(platform.configuration().configuredModels());
		return bean;
	}
}
