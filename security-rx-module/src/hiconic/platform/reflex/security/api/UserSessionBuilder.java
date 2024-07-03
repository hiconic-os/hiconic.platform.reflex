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

import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.securityservice.OpenUserSession;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.model.usersession.UserSessionType;

public interface UserSessionBuilder {

	UserSessionBuilder type(UserSessionType type);
	UserSessionBuilder request(OpenUserSession request);
	UserSessionBuilder requestContext(ServiceRequestContext requestContext);
	UserSessionBuilder internetAddress(String internetAddress);
	UserSessionBuilder expiryDate(Date expiryDate);
	UserSessionBuilder locale(String locale);
	UserSessionBuilder acquirationKey(String acquirationKey);
	UserSessionBuilder blocksAuthenticationAfterLogout(boolean blocksAuthenticationAfterLogout);
	UserSessionBuilder addProperty(String key, String value);
	UserSessionBuilder addProperties(Map<String, String> properties);

	Maybe<UserSession> buildFor(User user);
	Maybe<UserSession> buildFor(String userId, Set<String> roles);

}
