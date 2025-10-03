// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.security.web.processing.servlet.cookie;

import java.util.function.Function;

import com.braintribe.cfg.Configurable;
import com.braintribe.logging.Logger;
//import com.braintribe.model.processing.bootstrapping.TribefireRuntime;
import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;

import hiconic.rx.security.web.api.CookieHandler;
import hiconic.rx.security.web.api.Cookies;
import hiconic.rx.security.web.processing.WebSecurityConstants;
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
		String sessionCookieName = entryPoint != null? entryPoint.sessionCookieName(): WebSecurityConstants.COOKIE_SESSIONID;
		
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
		String sessionCookieName = entryPoint != null? entryPoint.sessionCookieName(): WebSecurityConstants.COOKIE_SESSIONID;

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
				log.trace(WebSecurityConstants.TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED
						+ " is disabled. Any user-session cookie will be invalid after a browser restart.");
			return -1;
		}

		if (!staySignedInRequested) {
			return -1;
		}

		String staySigned = req.getParameter(WebSecurityConstants.REQUEST_PARAM_STAYSIGNED);
		if (staySigned != null && staySigned.equals(Boolean.TRUE.toString())) {
			staySignedInRequested = true;
		}

		if (staySignedInRequested) {
			return cookieExpiry;
		} else {
			return -1;
		}
	}

	// TODO resolve OFFER_STAY_SIGNED flag...
	protected static boolean offerStaySigned() {
		return true;
//		String offerStayLoggedIn = TribefireRuntime.getProperty(WebSecurityConstants.TRIBEFIRE_RUNTIME_OFFER_STAYSIGNED);
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
