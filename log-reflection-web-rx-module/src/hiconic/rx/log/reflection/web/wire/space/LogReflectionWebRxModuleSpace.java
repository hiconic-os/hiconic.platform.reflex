package hiconic.rx.log.reflection.web.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebServerContract;

@Managed
public class LogReflectionWebRxModuleSpace implements RxModuleContract {
	public static final String WEBAPP_PATH = "log-reflection";

	@Import
	private WebServerContract webServer;

	@Import
	private RxPlatformContract platform;

	@Override
	public void onDeploy() {
		webServer.addWebAppRuntimeConfiguration(WEBAPP_PATH,
				() -> java.util.Map.of("adminRoles", platform.auth().roleAuthorization().adminRoles()));
	}
}
