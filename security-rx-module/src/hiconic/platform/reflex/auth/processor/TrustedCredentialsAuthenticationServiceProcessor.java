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
package hiconic.platform.reflex.auth.processor;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.credentials.TrustedCredentials;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.User;

/**
 * <p>
 * Authentication is granted only for trusted requests.
 * 
 */
public class TrustedCredentialsAuthenticationServiceProcessor extends BasicAuthenticateCredentialsServiceProcessor<TrustedCredentials>
		implements UserIdentificationValidation {

	private static Logger log = Logger.getLogger(TrustedCredentialsAuthenticationServiceProcessor.class);

	@Override
	protected Maybe<AuthenticateCredentialsResponse> authenticateCredentials(ServiceRequestContext requestContext, AuthenticateCredentials request,
			TrustedCredentials credentials) {

		Reason reason = validateCredentials(requestContext, credentials);

		if (reason != null)
			return Maybe.empty(reason);

		UserIdentification userIdentification = credentials.getUserIdentification();

		if (userIdentification == null)
			return Reasons.build(InvalidCredentials.T).text("TrustedCredentials.userIdentification must not be null").toMaybe();

		Maybe<User> userMaybe = retrieveUser(userIdentification);

		if (userMaybe.isUnsatisfied()) {
			log.debug(() -> "Authentication failed: " + userMaybe.whyUnsatisfied().stringify());
			return Reasons.build(InvalidCredentials.T).text("Invalid credentials").toMaybe();
		}

		User user = userMaybe.get();

		log.debug(() -> "Valid user [ " + user.getName() + " ] found based on the given trusted identification [ "
				+ credentials.getUserIdentification() + " ].");

		return Maybe.complete(buildAuthenticatedUserFrom(user));
	}

	private Reason validateCredentials(ServiceRequestContext requestContext, TrustedCredentials credentials) {
		if (!requestContext.isTrusted()) {
			return Reasons.build(InvalidCredentials.T).text("Trusted credentials are not allowed in this context.").toReason();
		}
		return validateUserIdentification(credentials.getUserIdentification());
	}

}
