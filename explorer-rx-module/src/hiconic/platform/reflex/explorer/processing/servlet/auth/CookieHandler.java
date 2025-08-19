// ============================================================================
package hiconic.platform.reflex.explorer.processing.servlet.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface CookieHandler {

	Cookie ensureCookie(HttpServletRequest req, HttpServletResponse resp, String sessionId);

	Cookie ensureCookie(HttpServletRequest req, HttpServletResponse resp, String sessionId, boolean staySignedIn);

	void invalidateCookie(HttpServletRequest req, HttpServletResponse resp, String sessionId);

}
