package hiconic.rx.security.web.api;

import hiconic.rx.module.api.wire.RxExportContract;

/**
 * @author peter.gazdik
 */
public interface WebSecurityContract extends RxExportContract {

	CookieHandler cookieHandler();

}
