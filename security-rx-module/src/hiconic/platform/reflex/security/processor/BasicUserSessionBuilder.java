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
package hiconic.platform.reflex.security.processor;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.securityservice.api.UserSessionService;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.OpenUserSession;
import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.user.Role;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;

import hiconic.platform.reflex.security.api.UserSessionBuilder;

public class BasicUserSessionBuilder implements UserSessionBuilder {

	private UserSessionService userSessionService;
	private TimeSpan maxIdleTime;
	private TimeSpan maxAge;

	private UserSessionType type = UserSessionType.normal;
	private OpenUserSession request;
	private ServiceRequestContext requestContext;
	private String internetAddress;
	private Date expiryDate;
	private Map<String, String> properties = new HashMap<>();
	private String locale;
	private String acquirationKey;
	private boolean blocksAuthenticationAfterLogout;

	public BasicUserSessionBuilder(UserSessionService userSessionService, TimeSpan maxIdleTime, TimeSpan maxAge) {
		this.userSessionService = userSessionService;
		this.maxIdleTime = maxIdleTime;
		this.maxAge = maxAge;
	}

	@Override
	public UserSessionBuilder acquirationKey(String acquirationKey) {
		this.acquirationKey = acquirationKey;
		return this;
	}

	@Override
	public UserSessionBuilder type(UserSessionType type) {
		this.type = type;
		return this;
	}

	@Override
	public UserSessionBuilder request(OpenUserSession request) {
		this.request = request;
		return this;
	}

	@Override
	public UserSessionBuilder requestContext(ServiceRequestContext requestContext) {
		this.requestContext = requestContext;
		return this;
	}

	@Override
	public UserSessionBuilder internetAddress(String internetAddress) {
		this.internetAddress = internetAddress;
		return this;
	}

	@Override
	public UserSessionBuilder expiryDate(Date expiryDate) {
		this.expiryDate = expiryDate;
		return this;
	}

	@Override
	public UserSessionBuilder locale(String locale) {
		this.locale = locale;
		return this;
	}

	@Override
	public UserSessionBuilder addProperty(String key, String value) {
		this.properties.put(key, value);
		return this;
	}

	@Override
	public UserSessionBuilder addProperties(Map<String, String> properties) {
		if (properties != null) {
			this.properties.putAll(properties);
		}
		return this;
	}

	@Override
	public UserSessionBuilder blocksAuthenticationAfterLogout(boolean blocksAuthenticationAfterLogout) {
		this.blocksAuthenticationAfterLogout = blocksAuthenticationAfterLogout;
		return this;
	}

	@Override
	public Maybe<UserSession> buildFor(User user) {

		if (this.internetAddress == null && this.requestContext != null) {
			this.internetAddress = this.requestContext.getRequestorAddress();
		}

		if (this.locale == null && this.request != null) {
			this.locale = this.request.getLocale();
		}

		if (this.locale != null) {
			this.properties.put("locale", this.locale);
		}
		Map<String, String> requestProperties = this.request.getProperties();
		if (requestProperties != null && !requestProperties.isEmpty()) {
			this.properties.putAll(requestProperties);
		}

		if (this.expiryDate == null) {
			this.expiryDate = this.request.getExpiryDate();
		}

		//@formatter:off
			return this.userSessionService
						.createUserSession(
								user, 
								this.type, 
								this.maxIdleTime, 
								this.maxAge, 
								this.expiryDate, 
								this.internetAddress, 
								this.properties,
								this.acquirationKey,
								this.blocksAuthenticationAfterLogout);
			//@formatter:on
	}

	@Override
	public Maybe<UserSession> buildFor(String userId, Set<String> roles) {
		return buildFor((createUser(userId, roles)));
	}

	private User createUser(String userId, Set<String> roles) {
		User user = User.T.create();
		user.setId(userId);
		user.setName(userId);

		if (roles != null) {
			user.setRoles(new HashSet<Role>());
			for (String roleId : roles) {
				Role role = Role.T.create();
				role.setId(roleId);
				role.setName(roleId);
				user.getRoles().add(role);
			}
		}
		return user;
	}

}