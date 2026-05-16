package hiconic.rx.security.web.api;

import hiconic.rx.module.api.config.RxPlatformConfigurator;
import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.module.api.wire.RxModuleContract;

/**
 * @author peter.gazdik
 */
public interface WebSecurityConfigurationContract extends RxExportContract {

	/**
	 * Sets the default login path, which can then be retrieved via {@link WebSecurityContract#defaultLoginPath()}. It is for example used by the
	 * Logout servlet as a redirect target after user logs out.
	 * <p>
	 * This method should be called during the {@link RxModuleContract#configurePlatform(RxPlatformConfigurator)} phase.
	 */
	void setDefaultLoginPath(String path);

}
