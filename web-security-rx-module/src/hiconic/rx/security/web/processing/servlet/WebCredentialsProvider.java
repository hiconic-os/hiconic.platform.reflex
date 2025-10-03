// ============================================================================
package hiconic.rx.security.web.processing.servlet;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.securityservice.credentials.Credentials;

import jakarta.servlet.http.HttpServletRequest;

public interface WebCredentialsProvider {
	Maybe<Credentials> provideCredentials(HttpServletRequest request);
}
