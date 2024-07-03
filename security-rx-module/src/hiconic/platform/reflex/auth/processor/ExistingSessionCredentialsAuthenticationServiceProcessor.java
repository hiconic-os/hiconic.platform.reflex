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
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.logging.Logger;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.ValidateUserSession;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;
import com.braintribe.model.securityservice.credentials.identification.UserNameIdentification;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.lcd.StringTools;

public class ExistingSessionCredentialsAuthenticationServiceProcessor
		extends BasicAuthenticateCredentialsServiceProcessor<ExistingSessionCredentials> {

	private static Logger log = Logger.getLogger(ExistingSessionCredentialsAuthenticationServiceProcessor.class);

	@Override
	protected Maybe<AuthenticateCredentialsResponse> authenticateCredentials(ServiceRequestContext context, AuthenticateCredentials request,
			ExistingSessionCredentials credentials) {

		if (StringTools.isBlank(credentials.getExistingSessionId())) {
			log.debug(() -> "Session id is empty in the given credentials: [ " + credentials + " ]");
			return Reasons.build(InvalidCredentials.T).text("ExistingSessionCredentials.existingSessionId must not be null or empty").toMaybe();
		}

		ValidateUserSession validateUserSession = ValidateUserSession.T.create();
		validateUserSession.setSessionId(credentials.getExistingSessionId());

		Maybe<? extends UserSession> userSessionMaybe = validateUserSession.eval(context).getReasoned();

		if (userSessionMaybe.isUnsatisfied()) {
			return userSessionMaybe.whyUnsatisfied().asMaybe();
		}

		UserSession userSession = userSessionMaybe.get();

		if (credentials.getReuseSession())
			return Maybe.complete(buildAuthenticatedUserSessionFrom(userSession));
		else {
			Maybe<User> userMaybe = retrieveUser(UserNameIdentification.of(userSession.getUser().getId()));

			if (userMaybe.isUnsatisfied()) {

				// An internal user session may not refer to a persisted user. We will use the one attached to the session instead
				if (userSession.getEffectiveRoles().contains("tf-internal")) {
					return Maybe.complete(buildAuthenticatedUserFrom(userSession.getUser()));
				}

				String msg = "User from existing session was not found in persistence for authentication";
				log.debug(() -> msg + ": " + userMaybe.whyUnsatisfied().stringify());
				return Reasons.build(InvalidCredentials.T).text(msg).toMaybe();
			}

			return Maybe.complete(buildAuthenticatedUserFrom(userMaybe.get()));
		}
	}

}
