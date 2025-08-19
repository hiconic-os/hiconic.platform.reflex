package hiconic.platform.reflex.explorer.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.platform.reflex.explorer.processing.servlet.alive.AliveServlet;
import hiconic.platform.reflex.explorer.processing.servlet.auth.AuthRxServlet;
import hiconic.platform.reflex.explorer.processing.servlet.auth.LoginRxServlet;
import hiconic.platform.reflex.explorer.processing.servlet.explorer.SymbolTranslationServlet;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebServerContract;

/**
 * @author peter.gazdik
 */
@Managed
public class WebappsSpace implements WireSpace {

	// @formatter:off
	@Import private RxPlatformContract platform;

	@Import private WebServerContract webServer;

	@Import private HttpRxSpace http;
	@Import private ServletsRxSpace servlets;
	// @formatter:on

	public void registerWebapps() {
		webServer.addStaticFileResource("tribefire-explorer", "web-app/tribefire-explorer", "ClientEntryPointRx.html");
		webServer.addStaticFileResource("tribefire-services/webpages", "web-app/tribefire-services/webpages");

		webServer.addServlet("tribefire-services", "alive-servlet", "/", aliveServlet());
		webServer.addServlet("tribefire-services", "login-servlet", "login", loginServlet());
		webServer.addServlet("login-auth-servlet", "login/auth", loginAuthServlet());

		// NOTE the "/" is super important, empty string would mean tribefire-explorer/symbolTranslation/ works, but without ending "/" it doesn't
		webServer.addServlet("tribefire-explorer/symbolTranslation", "SymbolTranslationServlet", "/", symbolTranslationServlet());
	}

	@Managed
	private AliveServlet aliveServlet() {
		AliveServlet bean = new AliveServlet();
		return bean;
	}

	@Managed
	public LoginRxServlet loginServlet() {
		LoginRxServlet bean = new LoginRxServlet();
		return bean;
	}

	@Managed
	public AuthRxServlet loginAuthServlet() {
		AuthRxServlet bean = new AuthRxServlet();
		bean.setCookieHandler(http.cookieHandler());
		bean.setRemoteAddressResolver(servlets.remoteAddressResolver());
		bean.setMarshallerRegistry(platform.marshallers());
		bean.setRequestEvaluator(platform.systemEvaluator());
		bean.setEntryPointProvider(http.openUserSessionConfigurationProvider()::findEntryPointName);

		return bean;
	}

	@Managed
	private SymbolTranslationServlet symbolTranslationServlet() {
		return new SymbolTranslationServlet();
	}

}
