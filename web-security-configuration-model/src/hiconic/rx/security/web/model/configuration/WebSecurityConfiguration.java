// ============================================================================
package hiconic.rx.security.web.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface WebSecurityConfiguration extends GenericEntity {

	EntityType<WebSecurityConfiguration> T = EntityTypes.T(WebSecurityConfiguration.class);

	/**
	 * Whether or not the standard login servlet at "/login" is enabled.
	 * <p>
	 * In case an SSO provider is used (Shiro module), we most likely don't want to also allows this user-password based login.
	 * <p>
	 * The default value is <code>true</code> if no other module configures the {@link #getLoginPath() default login path} programmatically.
	 * Otherwise, the default value is <code>false</code>.
	 */
	Boolean getStandardLoginEnabled();
	void setStandardLoginEnabled(Boolean standardLoginEnabled);

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
