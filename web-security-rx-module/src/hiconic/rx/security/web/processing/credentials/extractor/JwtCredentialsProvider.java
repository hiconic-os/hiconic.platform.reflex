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

		int typeSeparatorIndex = authHeader.indexOf(' ');

		if (typeSeparatorIndex == -1)
			return JWT_TOKEN_NOT_FOUND;

		String tokenType = authHeader.substring(0, typeSeparatorIndex).toLowerCase();

		if (tokenType.equals("bearer")) {
			String encodedToken = authHeader.substring(typeSeparatorIndex + 1).trim();
			logger.trace(() -> "Identified JWT token " + StringTools.simpleObfuscatePassword(encodedToken)
					+ " in the Authorization header of the request.");

			JwtTokenCredentials credentials = JwtTokenCredentials.of(encodedToken);
			// This ensures that we can reuse a session when the same token is received again.
			credentials.setAcquire(true);
			return Maybe.complete(credentials);
		}

		return JWT_TOKEN_NOT_FOUND;
	}

}
