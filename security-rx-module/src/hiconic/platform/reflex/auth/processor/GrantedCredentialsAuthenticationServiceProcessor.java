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

import java.util.Collections;
import java.util.Set;

import com.braintribe.cfg.Configurable;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.gm.model.security.reason.InvalidCredentials;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.securityservice.impl.Roles;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.OpenUserSessionResponse;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.GrantedCredentials;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;

/**
 * <p>
 * This experts authenticates the granting credentials as given by {@link GrantedCredentials#getGrantingCredentials()}
 * and returns a {@link OpenUserSessionResponse} for the user identified by
 * {@link GrantedCredentials#getUserIdentification()} .
 * 
 */
public class GrantedCredentialsAuthenticationServiceProcessor extends BasicAuthenticateCredentialsServiceProcessor<GrantedCredentials>
		implements UserIdentificationValidation {

	private static Logger log = Logger.getLogger(GrantedCredentialsAuthenticationServiceProcessor.class);

	private Set<EntityType<? extends Credentials>> unsupportedGrantingCredentialsTypes = Collections.emptySet();
	private Set<String> grantingRoles = Collections.<String> emptySet();

	@Configurable
	public void setUnsupportedGrantingCredentialsTypes(Set<EntityType<? extends Credentials>> unsupportedGrantingCredentialsTypes) {
		if (unsupportedGrantingCredentialsTypes == null) {
			unsupportedGrantingCredentialsTypes = Collections.emptySet();
		}
		this.unsupportedGrantingCredentialsTypes = unsupportedGrantingCredentialsTypes;
	}

	@Configurable
	public void setGrantingRoles(Set<String> grantingRoles) {
		if (grantingRoles == null) {
			grantingRoles = Collections.<String> emptySet();
		}
		this.grantingRoles = grantingRoles;
	}

	@Override
	protected Maybe<AuthenticateCredentialsResponse> authenticateCredentials(ServiceRequestContext context, AuthenticateCredentials request,
			GrantedCredentials credentials) {

		Reason reason = validateCredentials(credentials);

		if (reason != null)
			return reason.asMaybe();

		Credentials grantingCredentials = credentials.getGrantingCredentials();

		AuthenticateCredentials authenticateCredentials = AuthenticateCredentials.T.create();
		authenticateCredentials.setCredentials(grantingCredentials);

		Maybe<? extends AuthenticateCredentialsResponse> authenticatedCredentialsMaybe = authenticateCredentials.eval(context).getReasoned();

		if (authenticatedCredentialsMaybe.isUnsatisfied()) {
			log.debug("Granting credententials could not be authenticated: " + grantingCredentials);
			return Reasons.build(InvalidCredentials.T).text("Invalid Credentials").toMaybe();
		}

		AuthenticateCredentialsResponse authenticateCredentialsResponse = authenticatedCredentialsMaybe.get();

		reason = checkGrantingPermission(authenticateCredentialsResponse);

		if (reason != null)
			return reason.asMaybe();

		UserIdentification userIdentification = credentials.getUserIdentification();
		Maybe<User> userMaybe = retrieveUser(userIdentification);

		if (userMaybe.isUnsatisfied()) {
			log.debug("Granting credentials could not be authenticated because: " + userMaybe.whyUnsatisfied().stringify());
			return Reasons.build(InvalidCredentials.T).text("Invalid Credentials").toMaybe();
		}

		return Maybe.complete(buildAuthenticatedUserFrom(userMaybe.get()));
	}

	private Role roleFromStr(String roleName) {
		Role role = Role.T.create();
		role.setName(roleName);

		return role;
	}

	protected Reason checkGrantingPermission(AuthenticateCredentialsResponse authenticateCredentialsResponse) {

		Set<String> effectiveRoles = Roles.authenticatedCredentialsEffectiveRoles(authenticateCredentialsResponse);

		for (String grantingRole : grantingRoles) {
			if (effectiveRoles.contains(grantingRole))
				return null;
		}

		return Reasons.build(InvalidCredentials.T).text("Invalid Credentials")
				.cause(Reasons.build(Forbidden.T).text("Given granting credentials lack priviledge to granting authentication").toReason())
				.toReason();
	}

	private Reason validateCredentials(GrantedCredentials credentials) {

		Reason reason = validateUserIdentification(credentials.getUserIdentification());

		if (reason != null)
			return reason;

		Credentials grantingCredentials = credentials.getGrantingCredentials();

		if (grantingCredentials == null) {
			return Reasons.build(InvalidCredentials.T).text("CrantedCredentials.grantingCredentials must not be null").toReason();
		}

		for (EntityType<? extends Credentials> unsupportedGrantingCredentialsType : unsupportedGrantingCredentialsTypes) {
			if (unsupportedGrantingCredentialsType.isAssignableFrom(grantingCredentials.entityType())) {
				return Reasons.build(InvalidCredentials.T)
						.text("Unsupported type of granting credentials" + grantingCredentials.entityType().getTypeSignature()).toReason();
			}
		}

		return null;
	}

}
