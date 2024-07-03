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
package hiconic.platform.reflex.security.api;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.processing.securityservice.api.exceptions.SecurityServiceError;
import com.braintribe.model.processing.securityservice.api.exceptions.UserNotFoundException;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.User;

/**
 * <p>
 * Internal service to concentrate {@link User} related operations useful to components (authenticators / other internal
 * services) under the security service domain.
 * 
 */
public interface UserInternalService {

	/**
	 * Fetches a single {@link User} object from the underlying persistence layer where the property given by
	 * {@code propertyName} matches the value given by {@code propertyValue}.
	 * <p>
	 * {@code null} is returned if no {@link User} is found
	 * <p>
	 * If an assertive {@link User} retrieval is required, use {@link #retrieveUser(UserIdentification)}.
	 * 
	 * @param propertyName
	 *            Name of the property to be used for fetching the {@link User}
	 * @param propertyValue
	 *            Value of the property to be used for fetching the {@link User}
	 * @return the {@link User} object, if found. {@code null} otherwise
	 * @throws SecurityServiceError
	 *             if errors occur while accessing the underlying persistence layer
	 */
	User findUser(String propertyName, String propertyValue);

	/**
	 * Fetches a single {@link User} object from the underlying persistence layer using the identification given by the
	 * {@code userIdentification} parameter.
	 * <p>
	 * {@link UserNotFoundException} is thrown if no {@link User} is found
	 * 
	 * @param userIdentification
	 *            {@link UserIdentification} used for fetching the {@link User}
	 * @return the {@link User} found based on the given {@code userIdentification}
	 * @throws UserNotFoundException
	 *             if no {@link User} object matching the given {@code userIdentification} is found.
	 */
	User retrieveUser(UserIdentification userIdentification) throws UserNotFoundException;

	/**
	 * Fetches a single {@link User} object from the underlying persistence layer querying by identification given by the
	 * {@code userIdentification} parameter and the given password.
	 * <p>
	 * {@link UserNotFoundException} is thrown
	 * 
	 * @param userIdentification
	 *            {@link UserIdentification} used for fetching the {@link User}
	 * @param password
	 *            Password used for fetching the {@link User}
	 * @return the {@link User} found based on the given {@code userIdentification}
	 * @throws UserNotFoundException
	 *             if no {@link User} is found matching the given {@link UserIdentification} and password.
	 */
	User retrieveUser(UserIdentification userIdentification, String password) throws UserNotFoundException;

	/**
	 * Fetches a single {@link User} object from the underlying persistence layer where the property given by
	 * {@code propertyName} matches the value given by {@code propertyValue}.
	 * <p>
	 * {@link UserNotFoundException} is thrown if no {@link User} is found
	 * 
	 * @param propertyName
	 *            Name of the property to be used for fetching the {@link User}
	 * @param propertyValue
	 *            Value of the property to be used for fetching the {@link User}
	 * @return the {@link User} found based on the given {@code propertyName} and {@code propertyValue}
	 * @throws UserNotFoundException
	 *             if no {@link User} object matching the given {@code propertyName}/{@code propertyValue} is found.
	 * @throws SecurityServiceError
	 *             if errors occur while accessing the underlying persistence layer
	 */
	User retrieveUser(String propertyName, String propertyValue) throws UserNotFoundException;

	/**
	 * Fetches the id of the {@link User} based on the given identification.
	 * <p>
	 * This method may access the underlying persistence layer, if the user id is not part of the given
	 * {@link UserIdentification}.
	 * <p>
	 * {@link UserNotFoundException} is thrown if no {@link User} is found
	 * 
	 * @param userIdentification
	 *            {@link UserIdentification} used for fetching the {@link User} id
	 * @return the {@link User} id found based on the given {@link UserIdentification}
	 * @throws UserNotFoundException
	 *             if the underlying persistence layer is accessed and no {@link User} object matching the given
	 *             {@code propertyName}/{@code propertyValue} is found.
	 * @throws SecurityServiceError
	 *             if errors occur while accessing the underlying persistence layer
	 */
	String retrieveUserId(UserIdentification userIdentification) throws UserNotFoundException;

	/**
	 * Fetches the {@code name} property (unique, natural, user-provider key) of the {@link User} from the underlying
	 * persistence layer where the property given by {@code propertyName} matches the value given by {@code propertyValue}.
	 * <p>
	 * {@link UserNotFoundException} is thrown if no {@link User} is found
	 * 
	 * @param propertyName
	 *            Name of the property to be used for fetching the {@link User} name
	 * @param propertyValue
	 *            Value of the property to be used for fetching the {@link User} name
	 * @return the {@link User} {@code name} found based on the given {@code propertyName} and {@code propertyValue}
	 * @throws UserNotFoundException
	 *             if no {@link User} object matching the given {@code propertyName}/{@code propertyValue} is found.
	 * @throws SecurityServiceError
	 *             if errors occur while accessing the underlying persistence layer
	 */
	String retrieveUserName(String propertyName, String propertyValue) throws UserNotFoundException;

	Reason ensureUser(User user);
}
