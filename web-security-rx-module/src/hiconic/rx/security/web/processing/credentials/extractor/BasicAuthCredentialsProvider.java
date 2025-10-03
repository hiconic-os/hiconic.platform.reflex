// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.security.web.processing.credentials.extractor;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.logging.Logger;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.utils.StringTools;

public class BasicAuthCredentialsProvider implements CredentialFromAuthorizationHeaderProvider {

	private static Logger logger = Logger.getLogger(BasicAuthCredentialsProvider.class);
	private static final Maybe<Credentials> BASIC_TOKEN_NOT_FOUND = Reasons.build(NotFound.T)
			.text("HTTP Authorization header parameter did not contain a Basic token").toMaybe();

	@Override
	public Maybe<Credentials> provideCredentials(String authHeader) {
		String[] parts = authHeader.split("[ \\\r\\\n\\\t]");
		if (parts.length == 2) {
			String basic = parts[0].trim();
			String base64Encoded = parts[1].trim();

			if (basic.equalsIgnoreCase("Basic")) {
				byte[] decodedBytes;
				try {
					decodedBytes = Base64.getDecoder().decode(base64Encoded);
				} catch (Exception e) {
					logger.debug(() -> "Error while BASE64 decoding: " + base64Encoded);
					return Reasons.build(InvalidCredentials.T).text("Could not decode BASE64 encoding of Basic authorization token").toMaybe();
				}

				try {
					String credentials = new String(decodedBytes, "UTF-8");
					logger.debug(() -> "Credentials: " + StringTools.simpleObfuscatePassword(credentials));
					int p = credentials.indexOf(":");
					if (p != -1) {
						String login = credentials.substring(0, p).trim();
						String password = credentials.substring(p + 1).trim();

						UserPasswordCredentials creds = UserPasswordCredentials.forUserName(login, password);
						creds.setAcquire(true);
						return Maybe.complete(creds);
					} else {
						return Reasons.build(InvalidCredentials.T).text("Invalid Basic authorization token. Missing ':' separator.").toMaybe();
					}
				} catch (UnsupportedEncodingException e) {
					throw new UncheckedIOException(e);
				}
			}
		}

		return BASIC_TOKEN_NOT_FOUND;
	}

}
