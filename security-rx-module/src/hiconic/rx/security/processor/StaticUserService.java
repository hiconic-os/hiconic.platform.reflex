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
package hiconic.rx.security.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.securityservice.api.exceptions.UserNotFoundException;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.user.User;

import hiconic.rx.security.api.UserService;
import hiconic.rx.security.api.PasswordHashing;
import hiconic.rx.security.model.configuration.UserServiceConfiguration;

public class StaticUserService implements UserService {

	private final Map<String, User> users = new HashMap<>();
	private PasswordHashing passwordHashing;

	@Required
	public void setPasswordHashing(PasswordHashing passwordHashing) {
		this.passwordHashing = passwordHashing;
	}

	@Override
	public String userServiceId() {
		return UserServiceConfiguration.DEFAULT_USER_SERVICE_ID;
	}

	@Required
	public void setUsers(List<User> users) {
		for (User user : users) {
			if (user.getPassword() != null)
				user.setPassword(passwordHashing.hash(user.getPassword()));
			this.users.put(user.getName(), user);
		}
	}

	@Override
	public User findUser(String propertyName, String propertyValue) {
		if (propertyName.equals("name")) {
			return users.get(propertyValue);
		}

		Property p = User.T.getProperty(propertyName);

		return users.values().stream() //
				.filter(u -> propertyValue.equals(p.get(u))) //
				.findFirst() //
				.orElse(null);
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification, String password) throws UserNotFoundException {
		User user = retrieveUser(userIdentification);

		if (passwordHashing.matches(password, user.getPassword()))
			return user;

		throw new UserNotFoundException();
	}

	@Override
	public Reason ensureUser(User user) {
		return Reasons.build(UnsupportedOperation.T).text("Ensuring User ist not supported").toReason();
	}

}
