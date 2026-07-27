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

import java.util.LinkedHashMap;
import java.util.Map;

import com.braintribe.utils.StringTools;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.explorer.processing.servlet.about.AboutRxServlet;
import hiconic.rx.explorer.processing.servlet.about.expert.DiagnosticMultinode;
import hiconic.rx.explorer.processing.servlet.about.expert.Heapdump;
import hiconic.rx.explorer.processing.servlet.about.expert.HotThreadsExpert;
import hiconic.rx.explorer.processing.servlet.about.expert.Json;
import hiconic.rx.explorer.processing.servlet.about.expert.PackagingExpert;
import hiconic.rx.explorer.processing.servlet.about.expert.ProcessesExpert;
import hiconic.rx.explorer.processing.servlet.about.expert.SystemInformation;
import hiconic.rx.explorer.processing.servlet.about.expert.Threaddump;
import hiconic.rx.explorer.processing.servlet.alive.AliveServlet;
import hiconic.rx.explorer.processing.servlet.explorer.SymbolTranslationServlet;
import hiconic.rx.explorer.processing.servlet.explorer.ExplorerPublicResourceServlet;
import hiconic.rx.explorer.processing.servlet.explorer.UserImageServlet;
import hiconic.rx.explorer.processing.servlet.home.HomeRxServlet;
import hiconic.rx.explorer.processing.servlet.home.OpenApiLandingPageLinkConfigurer;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxServiceProcessingContract;
import hiconic.rx.security.web.api.AuthFilters;
import hiconic.rx.security.web.api.WebSecurityContract;
import hiconic.rx.web.server.api.WebServerContract;
import jakarta.servlet.DispatcherType;

/**
 * @author peter.gazdik
 */
@Managed
public class WebappsSpace implements WireSpace {
	private static final String EXPLORER_WEB_APP_PATH = "tribefire-explorer";
	private static final String LOG_REFLECTION_WEB_APP_PATH = "log-reflection";

	private static final Map<String, String> OPTIONAL_CLIENT_PROPERTIES = Map.of(
			"websocketUrl", "TRIBEFIRE_WEBSOCKET_URL",
			"controlCenterUrl", "TRIBEFIRE_CONTROL_CENTER_URL",
			"explorerUrl", "TRIBEFIRE_EXPLORER_URL",
			"tribefireJsUrl", "TRIBEFIRE_JS_URL",
			"platformSetupSupport", "TRIBEFIRE_PLATFORM_SETUP_SUPPORT",
			"webLoginRelativePath", "TRIBEFIRE_WEB_LOGIN_RELATIVE_PATH",
			"webReaderUrl", "TRIBEFIRE_WEBREADER_URL");

	// @formatter:off
	@Import private RxPlatformContract platform;
	@Import private RxServiceProcessingContract serviceProcessing;

	@Import private AccessContract access;
	@Import private WebServerContract webServer;
	@Import private WebSecurityContract webSecurity;
	// @formatter:on

	public void registerWebapps() {
		webServer.addWebAppRuntimeConfiguration(EXPLORER_WEB_APP_PATH, this::clientRuntimeProperties);
		webServer.addPackagedPublicResources("explorer-webpages", "webpages", "explorer/webpages");

		webServer.addServlet("alive-servlet", "/", aliveServlet());

		webServer.addServlet("home-servlet", "home", homeServlet());
		webServer.addFilterMapping(AuthFilters.lenientAuthFilter, "/home/*", DispatcherType.REQUEST);

		webServer.addServlet("explorer-public-resource-servlet", "publicResource/dynamic/*", explorerPublicResourceServlet());

		webServer.addServlet("user-image-servlet", "user-image/*", userImageServlet());
		webServer.addFilterMapping(AuthFilters.strictAuthFilter, "/user-image/*", DispatcherType.REQUEST);

		webServer.addServlet("about-servlet", "about", aboutServlet());
		webServer.addFilterMapping(AuthFilters.strictAdminAuthFilter, "/about/*", DispatcherType.REQUEST);

		// NOTE "/" (3rd arg) is important, empty string would mean tribefire-explorer/symbolTranslation/ works, but without ending "/" it doesn't
		webServer.addServlet("tribefire-explorer/symbolTranslation", "SymbolTranslationServlet", "/", symbolTranslationServlet());
	}

	private Map<String, String> clientRuntimeProperties() {
		Map<String, String> result = new LinkedHashMap<>();
		result.put("servicesUrl", webServer.defaultEndpointUrl());
		for (Map.Entry<String, String> property : OPTIONAL_CLIENT_PROPERTIES.entrySet()) {
			String value = platform.configuration().propertyResolver().resolve(property.getValue());
			if (value != null)
				result.put(property.getKey(), value);
		}
		return result;
	}

	@Managed
	private AliveServlet aliveServlet() {
		AliveServlet bean = new AliveServlet();
		// possibly make it configurable, used to be "TRIBEFIRE_LANDING_PAGE_URL"
		bean.setHomeRelativePath(webServer.resolveDefaultEndpointPath("home"));
		return bean;
	}

	@Managed
	private SymbolTranslationServlet symbolTranslationServlet() {
		return new SymbolTranslationServlet();
	}

	@Managed
	private ExplorerPublicResourceServlet explorerPublicResourceServlet() {
		ExplorerPublicResourceServlet bean = new ExplorerPublicResourceServlet();
		bean.setSessionFactory(access.systemSessionFactory());
		bean.setConfiguration(platform.configuration().readConfig(hiconic.rx.explorer.model.configuration.ExplorerConfiguration.T).get());
		return bean;
	}

	@Managed
	private UserImageServlet userImageServlet() {
		UserImageServlet bean = new UserImageServlet();
		bean.setSessionFactory(access.contextSessionFactory());
		bean.setDefaultUserImageUrl("/" + webServer.resolveDefaultEndpointPath("webpages/logo-user-default.png"));
		return bean;
	}

	@Managed
	private HomeRxServlet homeServlet() {
		HomeRxServlet bean = new HomeRxServlet();
		bean.setApplicationName(platform.application().applicationName());
		bean.setExplorerUrl("/tribefire-explorer"); // relative path works
		bean.setServiceDomains(serviceProcessing.serviceDomains());
		bean.setAccessDomains(access.accessDomains());
		bean.setGrantedRoles(platform.auth().roleAuthorization().adminRoles());
		bean.setRelativeLogPath("/" + LOG_REFLECTION_WEB_APP_PATH + "/");
		bean.setLogApplicationAvailable(() -> webServer.isWebAppRegistered(LOG_REFLECTION_WEB_APP_PATH));

		bean.addAccessLinkConfigurer(openApiLandingPageLinkConfigurer());
		bean.addServiceDomainLinkConfigurer(openApiLandingPageLinkConfigurer());

		String relativeSignInPath = webSecurity.defaultLoginPath();
		if (!StringTools.isBlank(relativeSignInPath)) {
			bean.setRelativeSignInPath(relativeSignInPath);
		}

		return bean;
	}

	@Managed
	private OpenApiLandingPageLinkConfigurer openApiLandingPageLinkConfigurer() {
		OpenApiLandingPageLinkConfigurer bean = new OpenApiLandingPageLinkConfigurer();
		return bean;
	}

	@Managed
	private AboutRxServlet aboutServlet() {
		AboutRxServlet bean = new AboutRxServlet();
		/*
		 * The HTTP boundary is protected by strictAdminAuthFilter. Requests created by the
		 * About page itself target the deliberately system-only internal service domain and
		 * therefore have to continue with the trusted system evaluator.
		 */
		bean.setRequestEvaluator(serviceProcessing.systemEvaluator());
		bean.setLiveInstances(platform.application().liveInstances());
		bean.setLocalInstanceId(platform.application().instanceId());
		bean.setExecutor(platform.execution().executorService());

		bean.setDiagnosticMultinode(aboutDiagnosticMultinode());
		bean.setThreaddump(aboutThreaddump());
		bean.setHeapdump(aboutHeapdump());
		bean.setJson(aboutJson());
		bean.setPackagingExpert(aboutPackagingExpert());
		bean.setHotThreadsExpert(aboutHotThreadsExpert());
		bean.setProcessesExpert(aboutProcessesExpert());
		bean.setSystemInformation(aboutSystemInformation());

		return bean;
	}

	@Managed
	private DiagnosticMultinode aboutDiagnosticMultinode() {
		DiagnosticMultinode bean = new DiagnosticMultinode();
		bean.setRequestEvaluator(serviceProcessing.systemEvaluator());
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

}
