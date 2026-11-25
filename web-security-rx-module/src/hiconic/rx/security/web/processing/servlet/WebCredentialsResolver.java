package hiconic.rx.security.web.processing.servlet;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.security.reason.MissingCredentials;
import com.braintribe.model.securityservice.credentials.Credentials;

import jakarta.servlet.http.HttpServletRequest;

/** Selects at most one applicable credential source for an HTTP request. */
final class WebCredentialsResolver {
	private WebCredentialsResolver() {
	}

	static Maybe<Credentials> resolve(HttpServletRequest request, Iterable<WebCredentialsProvider> providers) {
		for (WebCredentialsProvider provider : providers) {
			Maybe<Credentials> credentialsMaybe = provider.provideCredentials(request);

			if (credentialsMaybe.isSatisfied())
				return credentialsMaybe;

			if (credentialsMaybe.isUnsatisfiedBy(MissingCredentials.T) || credentialsMaybe.isUnsatisfiedBy(NotFound.T))
				continue;

			// Once a provider recognizes a malformed or otherwise unusable credential, its result is authoritative.
			// Falling through could silently replace an explicitly supplied credential with a lower-priority one.
			return credentialsMaybe;
		}

		return Reasons.build(MissingCredentials.T).text("No credentials found in http request").toMaybe();
	}
}
