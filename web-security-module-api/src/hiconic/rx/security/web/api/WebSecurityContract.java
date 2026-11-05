package hiconic.rx.security.web.api;

import hiconic.rx.module.api.wire.RxExportContract;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author peter.gazdik
 */
public interface WebSecurityContract extends RxExportContract {

	CookieHandler cookieHandler();

	/** Resolves the configured {@code OpenUserSession} entry point for the given HTTP request. */
	String openUserSessionEntryPointName(HttpServletRequest request);

	/** WebSecurityConfiguration#loginPath */
	String defaultLoginPath();

}
