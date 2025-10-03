// ============================================================================
package hiconic.rx.security.web.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface WebSecurityConfiguration extends GenericEntity {

	EntityType<WebSecurityConfiguration> T = EntityTypes.T(WebSecurityConfiguration.class);

	String getLoginPath();
	void setLoginPath(String loginPath);

	String getCookieDomain();
	void setCookieDomain(String cookieDomain);

	String getCookiePath();
	void setCookiePath(String cookiePath);

	@Initializer("true")
	boolean getCookieEnabled();
	void setCookieEnabled(boolean cookieEnabled);
}
