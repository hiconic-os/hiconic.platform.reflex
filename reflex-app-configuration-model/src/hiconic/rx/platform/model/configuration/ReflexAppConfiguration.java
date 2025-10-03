// ============================================================================
package hiconic.rx.platform.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ReflexAppConfiguration extends GenericEntity {

	EntityType<ReflexAppConfiguration> T = EntityTypes.T(ReflexAppConfiguration.class);

	String getApplicationId();
	void setApplicationId(String applicationId);

	String getNodeId();
	void setNodeId(String nodeId);

}
