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
package hiconic.rx.security.api;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.securityservice.AuthenticateCredentials;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.ExistingSessionCredentials;
import com.braintribe.model.securityservice.credentials.GrantedCredentials;
import com.braintribe.model.securityservice.credentials.TrustedCredentials;
import com.braintribe.model.securityservice.credentials.UserPasswordCredentials;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;

import hiconic.rx.module.api.wire.RxExportContract;

public interface SecurityExtensionContract extends RxExportContract {

	/**
	 * Registers a processor for handling {@link AuthenticateCredentials}.
	 * <p>
	 * The purpose of this processor is to provide a {@link User} instance (or even a {@link UserSession}) with properly set roles and groups.
	 * <p>
	 * The platform comes with processors for the following credentials:
	 * <ul>
	 * <li>{@link ExistingSessionCredentials}
	 * <li>{@link GrantedCredentials}
	 * <li>{@link UserPasswordCredentials}
	 * <li>{@link TrustedCredentials}
	 * </ul>
	 */
	<C extends Credentials> void registerCredentialProcessor(EntityType<C> credentialType,
			ReasonedServiceProcessor<? extends AuthenticateCredentials, AuthenticateCredentialsResponse> processor);

	void registerUserService(UserService userService);
}
