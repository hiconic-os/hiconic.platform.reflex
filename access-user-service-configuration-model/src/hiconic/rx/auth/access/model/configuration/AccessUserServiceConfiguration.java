// ============================================================================
package hiconic.rx.auth.access.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface AccessUserServiceConfiguration extends GenericEntity {

	EntityType<AccessUserServiceConfiguration> T = EntityTypes.T(AccessUserServiceConfiguration.class);

	@Initializer("'auth'")
	String getAuthAccessId();
	void setAuthAccessId(String authAccessId);

	/** Database used for cluster-safe provisioning revision state. */
	@Initializer("'auth'")
	String getProvisioningStateDatabaseId();
	void setProvisioningStateDatabaseId(String provisioningStateDatabaseId);

}
