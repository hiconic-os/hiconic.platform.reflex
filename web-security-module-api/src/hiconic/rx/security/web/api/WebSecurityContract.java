package hiconic.rx.security.web.api;

import hiconic.rx.module.api.wire.RxExportContract;
import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;
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

	/**
	 * Resolves the trusted, configured {@code OpenUserSession} entry point for the actual HTTP request.
	 * Endpoint integrations must pass this value through the server-side request context rather than
	 * copying its name to the client-controlled {@code OpenUserSession.entryPoint} property.
	 */
	OpenUserSessionEntryPoint openUserSessionEntryPoint(HttpServletRequest request);

	/** WebSecurityConfiguration#loginPath */
	String defaultLoginPath();

}
