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
package hiconic.rx.explorer.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.explorer.processing.servlet.about.AboutRxServlet;
import hiconic.rx.explorer.processing.servlet.about.expert.DiagnosticMultinode;
import hiconic.rx.explorer.processing.servlet.about.expert.Heapdump;
import hiconic.rx.explorer.processing.servlet.about.expert.HotThreadsExpert;
import hiconic.rx.explorer.processing.servlet.about.expert.Json;
import hiconic.rx.explorer.processing.servlet.about.expert.PackagingExpert;
import hiconic.rx.explorer.processing.servlet.about.expert.ProcessesExpert;
import hiconic.rx.explorer.processing.servlet.about.expert.SystemInformation;
import hiconic.rx.explorer.processing.servlet.about.expert.Threaddump;
import hiconic.rx.explorer.processing.servlet.about.expert.TribefireInformation;
import hiconic.rx.explorer.processing.servlet.alive.AliveServlet;
import hiconic.rx.explorer.processing.servlet.explorer.SymbolTranslationServlet;
import hiconic.rx.explorer.processing.servlet.home.HomeRxServlet;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.web.api.AuthFilters;
import hiconic.rx.topology.api.TopologyContract;
import hiconic.rx.web.server.api.WebServerContract;
import jakarta.servlet.DispatcherType;

/**
 * @author peter.gazdik
 */
@Managed
public class WebappsSpace implements WireSpace {

	// @formatter:off
	@Import private RxPlatformContract platform;

	@Import private TopologyContract topology;
	@Import private WebServerContract webServer;
	// @formatter:on

	public void registerWebapps() {
		String webpagesPath = webServer.resolveDefaultEndpointPath("webpages");

		webServer.addStaticFileResource("tribefire-explorer", "web-app/tribefire-explorer", "ClientEntryPointRx.html");
		webServer.addStaticFileResource(webpagesPath, "web-app/tribefire-services/webpages");

		webServer.addServlet("alive-servlet", "/", aliveServlet());

		webServer.addServlet("home-servlet", "home", homeServlet());
		webServer.addFilterMapping(AuthFilters.lenientAuthFilter, "/home/*", DispatcherType.REQUEST);

		webServer.addServlet("about-servlet", "about", aboutServlet());
		webServer.addFilterMapping(AuthFilters.strictAdminAuthFilter, "/about/*", DispatcherType.REQUEST);

		// NOTE "/" (3rd arg) is important, empty string would mean tribefire-explorer/symbolTranslation/ works, but without ending "/" it doesn't
		webServer.addServlet("tribefire-explorer/symbolTranslation", "SymbolTranslationServlet", "/", symbolTranslationServlet());
	}

	@Managed
	private AliveServlet aliveServlet() {
		AliveServlet bean = new AliveServlet();
		return bean;
	}

	@Managed
	private SymbolTranslationServlet symbolTranslationServlet() {
		return new SymbolTranslationServlet();
	}

	@Managed
	private HomeRxServlet homeServlet() {
		HomeRxServlet bean = new HomeRxServlet();
		bean.setApplicationName(platform.applicationName());

		return bean;
	}

	@Managed
	private AboutRxServlet aboutServlet() {
		AboutRxServlet bean = new AboutRxServlet();
		bean.setRequestEvaluator(platform.evaluator());
		bean.setLiveInstances(topology.liveInstances());
		bean.setLocalInstanceId(platform.instanceId());
		bean.setExecutor(platform.executorService());

		bean.setDiagnosticMultinode(aboutDiagnosticMultinode());
		bean.setThreaddump(aboutThreaddump());
		bean.setHeapdump(aboutHeapdump());
		bean.setJson(aboutJson());
		bean.setPackagingExpert(aboutPackagingExpert());
		bean.setHotThreadsExpert(aboutHotThreadsExpert());
		bean.setProcessesExpert(aboutProcessesExpert());
		bean.setSystemInformation(aboutSystemInformation());
		bean.setTribefireInformation(aboutTribefireInformation());

		return bean;
	}

	@Managed
	private DiagnosticMultinode aboutDiagnosticMultinode() {
		DiagnosticMultinode bean = new DiagnosticMultinode();
		bean.setRequestEvaluator(platform.evaluator());
		return bean;
	}
	@Managed
	private Threaddump aboutThreaddump() {
		Threaddump bean = new Threaddump();
		return bean;
	}
	@Managed
	private Heapdump aboutHeapdump() {
		Heapdump bean = new Heapdump();
		return bean;
	}
	@Managed
	private Json aboutJson() {
		Json bean = new Json();
		return bean;
	}
	@Managed
	private PackagingExpert aboutPackagingExpert() {
		PackagingExpert bean = new PackagingExpert();
		return bean;
	}
	@Managed
	private HotThreadsExpert aboutHotThreadsExpert() {
		HotThreadsExpert bean = new HotThreadsExpert();
		return bean;
	}
	@Managed
	private ProcessesExpert aboutProcessesExpert() {
		ProcessesExpert bean = new ProcessesExpert();
		return bean;
	}
	@Managed
	private SystemInformation aboutSystemInformation() {
		SystemInformation bean = new SystemInformation();
		return bean;
	}
	@Managed
	private TribefireInformation aboutTribefireInformation() {
		TribefireInformation bean = new TribefireInformation();
		return bean;
	}

}
