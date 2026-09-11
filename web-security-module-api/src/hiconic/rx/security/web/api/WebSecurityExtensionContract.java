package hiconic.rx.security.web.api;

import hiconic.rx.module.api.wire.RxExportContract;

public interface WebSecurityExtensionContract extends RxExportContract {
	void registerRequestContextContributor(WebSecurityRequestContextContributor contributor);
}
