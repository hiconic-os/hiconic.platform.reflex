// ============================================================================
package hiconic.rx.security.web.wire.space;

import java.util.function.Function;

import com.braintribe.model.security.service.config.OpenUserSessionConfiguration;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.web.model.configuration.WebSecurityConfiguration;
import hiconic.rx.security.web.processing.servlet.cookie.DefaultCookieHandler;
import hiconic.rx.security.web.processing.servlet.providers.OpenUserSessionConfigurationProviderImpl;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author peter.gazdik
 */
@Managed
public class HttpRxSpace implements WireSpace {

	@Import
	private RxPlatformContract platform;

	@Managed
	public DefaultCookieHandler cookieHandler() {
		WebSecurityConfiguration config = platform.readConfig(WebSecurityConfiguration.T).get();

		DefaultCookieHandler bean = new DefaultCookieHandler();
		bean.setCookieDomain(config.getCookieDomain());
		bean.setCookiePath(config.getCookiePath());
		// DEVCX-208: The Control Center cannot access the cookie anymore if the cookie is not reachable via JavaScript.
		bean.setAddCookie(config.getCookieEnabled());

		bean.setAddCookie(true);
		bean.setEntryPointProvider(openUserSessionConfigurationProvider()::findEntryPoint);
		return bean;
	}

	@Managed
	public OpenUserSessionConfigurationProviderImpl openUserSessionConfigurationProvider() {
		OpenUserSessionConfigurationProviderImpl bean = new OpenUserSessionConfigurationProviderImpl();
		bean.setOpenUserSessionConfiguration(platform.readConfig(OpenUserSessionConfiguration.T).get());
		return bean;
	}

	public Function<HttpServletRequest, String> sessionCookieNameSupplier() {
		return openUserSessionConfigurationProvider()::getCookieName;
	}

}
