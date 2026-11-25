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

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.logging.Logger;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.JwtTokenCredentials;
import com.braintribe.utils.lcd.StringTools;

public class JwtCredentialsProvider implements CredentialFromAuthorizationHeaderProvider {

	private static Logger logger = Logger.getLogger(JwtCredentialsProvider.class);
	private static final Maybe<Credentials> JWT_TOKEN_NOT_FOUND = Reasons.build(NotFound.T)
			.text("HTTP Authorization header parameter did not contain a JWT token").toMaybe();

	@Override
	public Maybe<Credentials> provideCredentials(String authHeader) {
		authHeader = authHeader.trim();

		int typeSeparatorIndex = firstWhitespace(authHeader);
		String tokenType = typeSeparatorIndex == -1 ? authHeader : authHeader.substring(0, typeSeparatorIndex);

		if (tokenType.equalsIgnoreCase("bearer")) {
			if (typeSeparatorIndex == -1)
				return Reasons.build(InvalidCredentials.T).text("Bearer token is missing").toMaybe();

			String encodedToken = authHeader.substring(typeSeparatorIndex + 1).trim();
			if (encodedToken.isEmpty())
				return Reasons.build(InvalidCredentials.T).text("Bearer token is empty").toMaybe();

			logger.trace(() -> "Identified JWT token " + StringTools.simpleObfuscatePassword(encodedToken)
					+ " in the Authorization header of the request.");

			JwtTokenCredentials credentials = JwtTokenCredentials.of(encodedToken);
			// This ensures that we can reuse a session when the same token is received again.
			credentials.setAcquire(true);
			return Maybe.complete(credentials);
		}

		return JWT_TOKEN_NOT_FOUND;
	}

	private static int firstWhitespace(String value) {
		for (int i = 0; i < value.length(); i++)
			if (Character.isWhitespace(value.charAt(i)))
				return i;
		return -1;
	}

}
