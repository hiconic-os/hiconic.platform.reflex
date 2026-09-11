package hiconic.rx.browser.acceptance.processing;

import java.util.UUID;

import com.braintribe.common.attribute.AttributeContextBuilder;
import hiconic.rx.security.web.api.WebSecurityRequestContextContributor;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BrowserContextCookieContributor implements WebSecurityRequestContextContributor {
	private final String cookieName;
	private final int maxAge;

	public BrowserContextCookieContributor(String cookieName, int maxAge) {
		this.cookieName = cookieName;
		this.maxAge = maxAge;
	}

	@Override
	public void contribute(HttpServletRequest request, HttpServletResponse response, AttributeContextBuilder contextBuilder) {
		String token = find(request);
		if (token == null || token.isBlank()) {
			token = UUID.randomUUID().toString();
			response.addHeader("Set-Cookie", cookieName + "=" + token + "; Path=/; Max-Age=" + maxAge
					+ "; Secure; HttpOnly; SameSite=Lax");
		}
		contextBuilder.set(BrowserContextIdAttribute.class, token);
		contextBuilder.set(BrowserRequestInformationAttribute.class, new BrowserRequestInformation( //
				request.getRemoteAddr(), //
				request.getHeader("User-Agent"), //
				request.getHeader("Sec-CH-UA"), //
				request.getHeader("Sec-CH-UA-Platform"), //
				request.getHeader("Sec-CH-UA-Mobile")));
	}

	private String find(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;
		for (Cookie cookie : cookies)
			if (cookieName.equals(cookie.getName())) return cookie.getValue();
		return null;
	}
}
