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
