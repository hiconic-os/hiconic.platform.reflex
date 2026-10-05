// ============================================================================
package hiconic.rx.platform.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ReflexAppConfiguration extends GenericEntity {

	EntityType<ReflexAppConfiguration> T = EntityTypes.T(ReflexAppConfiguration.class);

	/**
	 * Technical identifier of the application.
	 * <p>
	 * Use only letters, numbers, underscores and dashes.
	 * <p>
	 * If not configured the application name will be used to generate an application id, by replacing all non-alphanumeric characters with dashes.
	 */
	String getApplicationId();
	void setApplicationId(String applicationId);

	String getNodeId();
	void setNodeId(String nodeId);

}
