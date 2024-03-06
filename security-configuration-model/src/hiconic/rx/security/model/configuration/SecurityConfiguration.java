// ============================================================================
package hiconic.rx.security.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface SecurityConfiguration extends GenericEntity {

	EntityType<SecurityConfiguration> T = EntityTypes.T(SecurityConfiguration.class);

	@Initializer("'user-sessions'")
	String getUserSessionDb();
	void setUserSessionDb(String userSessionDb);
	
}
