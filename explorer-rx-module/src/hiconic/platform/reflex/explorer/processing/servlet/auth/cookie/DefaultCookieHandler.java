// ============================================================================
package hiconic.platform.reflex.explorer.processing.servlet.auth.cookie;

import java.util.function.Function;

import com.braintribe.cfg.Configurable;
import com.braintribe.logging.Logger;
//import com.braintribe.model.processing.bootstrapping.TribefireRuntime;
import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;

import hiconic.platform.reflex.explorer.processing.servlet.auth.Constants;
import hiconic.platform.reflex.explorer.processing.servlet.auth.CookieHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DefaultCookieHandler implements CookieHandler {

	private static final Logger log = Logger.getLogger(DefaultCookieHandler.class);

	private int cookieExpiry = 24 * 60 * 60; // 24h
	private String cookiePath = null;
	private String cookieDomain = null;
	private boolean addSessionCookie = true;
	private Function<HttpServletRequest, OpenUserSessionEntryPoint> entryPointProvider = r -> null;

	@Override
	public Cookie ensureCookie(HttpServletRequest req, HttpServletResponse resp, String sessionId) {
		return this.ensureCookie(req, resp, sessionId, false);
	}

	@Override
	public Cookie ensureCookie(HttpServletRequest req, HttpServletResponse resp, String sessionId, boolean staySignedIn) {
		if (!addSessionCookie) {
			log.trace(() -> "Session cookies are disabled.");
			return null;
		}
		
		OpenUserSessionEntryPoint entryPoint = entryPointProvider.apply(req);

		Cookie sessionCookie = acquireCookie(req, entryPoint, sessionId);
		
		sessionCookie.setMaxAge(getMaxAge(req, staySignedIn));
		
		configureSessionCookieCommonsAndAdd(req, resp, entryPoint, sessionCookie);

		return sessionCookie;
	}
	
	@Override
	public void invalidateCookie(HttpServletRequest req, HttpServletResponse resp, String sessionId) {
		OpenUserSessionEntryPoint entryPoint = entryPointProvider.apply(req);
		String sessionCookieName = entryPoint != null? entryPoint.sessionCookieName(): Constants.COOKIE_SESSIONID;
		
		Cookie sessionCookie = Cookies.findCookie(req, sessionCookieName);
		
		if (sessionCookie != null) {
			sessionCookie.setMaxAge(0);
			sessionCookie.setValue("");

			configureSessionCookieCommonsAndAdd(req, resp, entryPoint, sessionCookie);
		}
	}

	private void configureSessionCookieCommonsAndAdd(HttpServletRequest req, HttpServletResponse resp, OpenUserSessionEntryPoint entryPoint, Cookie sessionCookie) {
		String cookiePath = this.cookiePath;
		if (cookiePath == null) {
			cookiePath = "/";
		}

		sessionCookie.setPath(cookiePath);
		sessionCookie.setHttpOnly(entryPoint != null? entryPoint.getSessionCookieHttpOnly(): false);

		if (this.cookieDomain != null) {
			sessionCookie.setDomain(this.cookieDomain);
		}

		String scheme = req != null ? getScheme(req) : "https";
		
		boolean isHttps = scheme != null && scheme.equalsIgnoreCase("https"); 
		
		if (isHttps) {
			// only in case of HTTPS it is possible to support cross domain cookies 
			// but this requires a Secure and a SameSite=None attribution
			sessionCookie.setSecure(true);
			addCookieWithSameSite(resp, sessionCookie, "None");
		}
		else {
			resp.addCookie(sessionCookie);
		}
		
	}

	
    public static void addCookieWithSameSite(HttpServletResponse response, Cookie cookie, String sameSite) {
        StringBuilder header = new StringBuilder();

        header.append(cookie.getName()).append("=").append(cookie.getValue() != null ? cookie.getValue() : "");

        if (cookie.getPath() != null) {
            header.append("; Path=").append(cookie.getPath());
        }

        if (cookie.getDomain() != null) {
            header.append("; Domain=").append(cookie.getDomain());
        }

        if (cookie.getMaxAge() >= 0) {
            header.append("; Max-Age=").append(cookie.getMaxAge());
        }

        if (cookie.getSecure()) {
            header.append("; Secure");
        }

        if (cookie.isHttpOnly()) {
            header.append("; HttpOnly");
        }

        if (sameSite != null && !sameSite.isEmpty()) {
            header.append("; SameSite=").append(sameSite);
        }

        response.addHeader("Set-Cookie", header.toString());
    }


	private String getScheme(HttpServletRequest req) {
		String proxyProto = req.getHeader("X-Forwarded-Proto");
		if (proxyProto != null) {
			log.debug("X-Forwarded-Proto header from proxies: " + proxyProto);
			return proxyProto;
		}
		
		return req.getScheme();
	}

	private Cookie acquireCookie(HttpServletRequest req, OpenUserSessionEntryPoint entryPoint, String sessionId) throws RuntimeException {
		String sessionCookieName = entryPoint != null? entryPoint.sessionCookieName(): Constants.COOKIE_SESSIONID;

		Cookie sessionCookie = req != null ? Cookies.findCookie(req, sessionCookieName) : null;
		if (sessionCookie == null) {
			sessionCookie = new Cookie(sessionCookieName, sessionId);
		} else {
			sessionCookie.setValue(sessionId);
		}
		return sessionCookie;
	}

	private int getMaxAge(HttpServletRequest req, boolean staySignedInRequested) {

		if (!offerStaySigned()) {
			if (log.isTraceEnabled())
				log.trace(Constants.TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED
						+ " is disabled. Any user-session cookie will be invalid after a browser restart.");
			return -1;
		}

		if (!staySignedInRequested) {
			return -1;
		}

		String staySigned = req.getParameter(Constants.REQUEST_PARAM_STAYSIGNED);
		if (staySigned != null && staySigned.equals(Boolean.TRUE.toString())) {
			staySignedInRequested = true;
		}

		if (staySignedInRequested) {
			return cookieExpiry;
		} else {
			return -1;
		}
	}

	protected static boolean offerStaySigned() {
		return true;
//		String offerStayLoggedIn = TribefireRuntime.getProperty(Constants.TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED);
//		if (offerStayLoggedIn != null && offerStayLoggedIn.equalsIgnoreCase("false")) {
//			return false;
//		} else {
//			return true;
//		}
	}

	@Configurable
	public void setCookieExpiry(int cookieExpiry) {
		this.cookieExpiry = cookieExpiry;
	}
	@Configurable
	public void setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
	}
	@Configurable
	public void setCookieDomain(String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}
	@Configurable
	public void setAddCookie(boolean addCookie) {
		this.addSessionCookie = addCookie;
	}
	@Configurable
	public void setEntryPointProvider(Function<HttpServletRequest, OpenUserSessionEntryPoint> entryPointProvider) {
		this.entryPointProvider = entryPointProvider;
	}
}
