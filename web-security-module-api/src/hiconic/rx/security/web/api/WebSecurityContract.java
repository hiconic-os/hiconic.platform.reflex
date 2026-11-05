package hiconic.rx.security.web.api;

import hiconic.rx.module.api.wire.RxExportContract;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author peter.gazdik
 */
public interface WebSecurityContract extends RxExportContract {

	CookieHandler cookieHandler();

	/** Shared login intervention flow used by native and contributed authentication mechanisms. */
	LoginFlow loginFlow();

	/** Resolves the configured {@code OpenUserSession} entry point for the given HTTP request. */
	String openUserSessionEntryPointName(HttpServletRequest request);

	/** WebSecurityConfiguration#loginPath */
	String defaultLoginPath();

}
