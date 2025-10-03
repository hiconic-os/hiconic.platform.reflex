// ============================================================================
package hiconic.rx.security.web.processing.credentials.extractor;

import java.util.Optional;
import java.util.function.Function;

import com.braintribe.cfg.Configurable;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.logging.Logger;

import hiconic.rx.security.web.api.Cookies;
import hiconic.rx.security.web.processing.WebSecurityConstants;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class ExistingSessionFromCookieProvider implements ExistingSessionWebCredentialProvider {
	private static final Logger logger = Logger.getLogger(ExistingSessionFromCookieProvider.class);
	
	private Function<HttpServletRequest, String> sessionCookieNameProvider = r -> WebSecurityConstants.COOKIE_SESSIONID;

	@Configurable
	public void setSessionCookieNameProvider(Function<HttpServletRequest, String> sessionCookieNameProvider) {
		this.sessionCookieNameProvider = sessionCookieNameProvider;
	}
	
	@Override
	public Maybe<String> findSessionId(HttpServletRequest request) {
		String cookieName = sessionCookieNameProvider.apply(request);
		
		String sessionId  = Optional.ofNullable(Cookies.findCookie(request, cookieName)).map(Cookie::getValue).orElse(null);
		
		if (sessionId == null || sessionId.isEmpty())
			return Reasons.build(NotFound.T).text("HTTP cookie " + cookieName + " not present")
					.toMaybe();
		
		logger.debug(() -> "Found session cookie with name [" + cookieName + "]");

		return Maybe.complete(sessionId);
	}

}
