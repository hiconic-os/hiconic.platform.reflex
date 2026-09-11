package hiconic.rx.browser.acceptance.web.wire.space;

import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.browser.acceptance.model.configuration.BrowserAcceptanceConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebAppNavigationEntry;
import hiconic.rx.web.server.api.WebServerContract;

@Managed
public class BrowserAcceptanceWebRxModuleSpace implements RxModuleContract {
	private static final String WEBAPP_PATH = "browser-acceptance";

	@Import
	private WebServerContract webServer;

	@Import
	private RxPlatformContract platform;

	@Override
	public void onDeploy() {
		BrowserAcceptanceConfiguration configuration = UnsatisfiedMaybeTunneling.getOrTunnel(
				platform.configuration().readConfig(BrowserAcceptanceConfiguration.T));
		if (!configuration.getEnabled())
			return;

		webServer.addWebAppRuntimeConfiguration(WEBAPP_PATH);
		webServer.addWebAppNavigation(new WebAppNavigationEntry(WEBAPP_PATH, "Device Approvals",
				"Approve and manage devices used to access the platform.", configuration.getApproverRoles(), 50));
	}
}
