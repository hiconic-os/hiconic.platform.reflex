package hiconic.rx.security.web.api;

import hiconic.rx.module.api.wire.RxExportContract;

public interface WebSecurityExtensionContract extends RxExportContract {
	void registerRequestContextContributor(WebSecurityRequestContextContributor contributor);
	void registerLoginFlowExtension(LoginFlowExtension extension);

	/**
	 * Registers an authentication alternative for presentation on the standard login page. Option ids must be unique.
	 */
	void registerLoginOption(LoginOption option);
}
