package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * Selects a single node or, when nodeId is absent, all nodes of an application.
 * {@code *} can be used as application id to address every application.
 */
public interface LogTarget extends GenericEntity {
	EntityType<LogTarget> T = EntityTypes.T(LogTarget.class);

	String getApplicationId();
	void setApplicationId(String applicationId);

	String getNodeId();
	void setNodeId(String nodeId);
}
