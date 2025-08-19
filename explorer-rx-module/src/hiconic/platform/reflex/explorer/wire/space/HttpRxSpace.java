package hiconic.platform.reflex.explorer.wire.space;

import com.braintribe.model.security.service.config.OpenUserSessionConfiguration;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.platform.reflex.explorer.processing.servlet.auth.cookie.DefaultCookieHandler;
import hiconic.platform.reflex.explorer.processing.servlet.auth.providers.OpenUserSessionConfigurationProviderImpl;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * @author peter.gazdik
 */
@Managed
public class HttpRxSpace implements WireSpace {

	@Import
	private RxPlatformContract platform;

	
	@Managed
	public DefaultCookieHandler cookieHandler() {
		DefaultCookieHandler bean = new DefaultCookieHandler();
//		bean.setCookieDomain(TribefireRuntime.getCookieDomain());
//		bean.setCookiePath(TribefireRuntime.getCookiePath());
		// DEVCX-208: The Control Center cannot access the cookie anymore if the cookie is not reachable via JavaScript.
		//bean.setAddCookie(TribefireRuntime.getCookieEnabled());
		
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
	

	
}
