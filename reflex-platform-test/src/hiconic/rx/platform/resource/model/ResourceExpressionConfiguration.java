// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.resource.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.Resource;

public interface ResourceExpressionConfiguration extends GenericEntity {

	EntityType<ResourceExpressionConfiguration> T = EntityTypes.T(ResourceExpressionConfiguration.class);

	String getText();
	void setText(String text);

	Resource getResource();
	void setResource(Resource resource);
}
