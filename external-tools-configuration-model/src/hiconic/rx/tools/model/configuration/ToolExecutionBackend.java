package hiconic.rx.tools.model.configuration;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ToolExecutionBackend extends GenericEntity {

	EntityType<ToolExecutionBackend> T = EntityTypes.T(ToolExecutionBackend.class);

}
