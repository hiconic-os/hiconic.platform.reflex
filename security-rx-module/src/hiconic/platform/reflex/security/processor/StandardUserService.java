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
package hiconic.platform.reflex.security.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.securityservice.api.exceptions.UserNotFoundException;
import com.braintribe.model.securityservice.credentials.identification.EmailIdentification;
import com.braintribe.model.securityservice.credentials.identification.UserIdentification;
import com.braintribe.model.securityservice.credentials.identification.UserNameIdentification;
import com.braintribe.model.user.User;

import hiconic.platform.reflex.security.api.UserInternalService;

public class StandardUserService implements UserInternalService {
	
	private Map<String, User> users = new HashMap<>();
	
	@Required
	public void setUsers(List<User> users) {
		for (User user: users) {
			this.users.put(user.getName(), user);
		}
	}

	@Override
	public User findUser(String propertyName, String propertyValue) {
		if (propertyName.equals("name")) {
			return users.get(propertyValue);
		}
		
		Property p = User.T.findProperty(propertyName);
		
		return users.values().stream().filter(u -> propertyValue.equals(p.get(u))).findFirst().orElse(null);
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification) throws UserNotFoundException {
		if (userIdentification instanceof EmailIdentification) {
			EmailIdentification ei = (EmailIdentification)userIdentification;
			return retrieveUser(User.email, ei.getEmail());
		}
		else if (userIdentification instanceof UserNameIdentification) {
			UserNameIdentification ni = (UserNameIdentification)userIdentification;
			return retrieveUser(User.name, ni.getUserName());
		}
		else
			throw new UnsupportedOperationException("Unsupported UserIdentification " + userIdentification.getClass());
	}

	@Override
	public User retrieveUser(UserIdentification userIdentification, String password) throws UserNotFoundException {
		User user = retrieveUser(userIdentification);
		
		if (password.equals(user.getPassword()))
			return user;
		
		throw new UserNotFoundException();
	}

	@Override
	public User retrieveUser(String propertyName, String propertyValue) throws UserNotFoundException {
		User user = findUser(propertyName, propertyValue);
		if (user == null)
			throw new UserNotFoundException();
		
		return user;
	}

	@Override
	public String retrieveUserId(UserIdentification userIdentification) throws UserNotFoundException {
		return retrieveUser(userIdentification).getId();
	}

	@Override
	public String retrieveUserName(String propertyName, String propertyValue) throws UserNotFoundException {
		return retrieveUser(propertyName, propertyValue).getName();
	}

	@Override
	public Reason ensureUser(User user) {
		return Reasons.build(UnsupportedOperation.T).text("Ensuring User persistence ist not supported").toReason();
	}
	

}
