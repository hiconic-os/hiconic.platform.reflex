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
// ============================================================================
package hiconic.platform.reflex.auth.processor;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.processing.securityservice.api.exceptions.UserNotFoundException;
import com.braintribe.model.processing.securityservice.impl.AbstractAuthenticateCredentialsServiceProcessor;
import com.braintribe.model.securityservice.credentials.Credentials;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.User;

import hiconic.platform.reflex.security.api.UserInternalService;

/**
 * <p>
 * Abstraction for authentication experts.
 * 
 * @param <T>
 *            The type of {@link Credentials} the expert handles.
 */
public abstract class BasicAuthenticateCredentialsServiceProcessor<T extends Credentials> extends AbstractAuthenticateCredentialsServiceProcessor<T> {

	private UserInternalService userService;
	
	@Required
	public void setUserService(UserInternalService userService) {
		this.userService = userService;
	}

	/**
	 * <p>
	 * Fetches a single user from the authentication access using the identification given by the {@code userIdentification}
	 * parameter.
	 * <p>
	 * This method will retrieve and discard a {@link PersistenceGmSession} for accessing the authentication data.
	 * 
	 * @param userIdentification
	 *            {@link UserIdentification} used for fetching a user from the authentication access
	 * @return the {@link User} found based on the given {@code userIdentification}
	 */
	protected Maybe<User> retrieveUser(UserIdentification userIdentification) {
		try {
			return Maybe.complete(userService.retrieveUser(userIdentification));
		} catch (UserNotFoundException e) {
			return Reasons.build(NotFound.T).text("Missing user with identification: " + userIdentification).toMaybe();
		}
	}

	/**
	 * <p>
	 * Fetches a single user from the authentication access using the identification given by the {@code userIdentification}
	 * parameter.
	 * <p>
	 * This method will retrieve and discard a {@link PersistenceGmSession} for accessing the authentication data.
	 * 
	 * @param userIdentification
	 *            {@link UserIdentification} used for fetching a user from the authentication access
	 * @return the {@link User} found based on the given {@code userIdentification}
	 */
	protected Maybe<User> retrieveUser(UserIdentification userIdentification, String password) {
		try {
			return Maybe.complete(userService.retrieveUser(userIdentification, password));
		} catch (UserNotFoundException e) {
			return Reasons.build(NotFound.T).text("Missing user with identification: " + userIdentification).toMaybe();
		}
	}

	/**
	 * <p>
	 * Fetches a single user id from the authentication access using the identification given by the
	 * {@code userIdentification} parameter.
	 * <p>
	 * This method will use the given {@link PersistenceGmSession} for accessing the authentication data.
	 * 
	 * @param gmSession
	 *            {@link PersistenceGmSession} used for accessing the authentication access
	 * @param userIdentification
	 *            {@link UserIdentification} used for fetching a user from the authentication access
	 * @return the {@link User} id found based on the given {@code userIdentification}
	 * @throws UserNotFoundException
	 *             if an unique user is not found
	 */
	protected Maybe<String> retrieveUserId(UserIdentification userIdentification) {
		try {
			return Maybe.complete(userService.retrieveUserId(userIdentification));
		} catch (UserNotFoundException e) {
			return Reasons.build(NotFound.T).text("Missing user with identification: " + userIdentification).toMaybe();
		}
	}


}
