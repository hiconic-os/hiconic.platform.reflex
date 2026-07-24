package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogOrigin extends GenericEntity {
	EntityType<LogOrigin> T = EntityTypes.T(LogOrigin.class);

	String getApplicationId();
	void setApplicationId(String applicationId);

	String getNodeId();
	void setNodeId(String nodeId);
}
