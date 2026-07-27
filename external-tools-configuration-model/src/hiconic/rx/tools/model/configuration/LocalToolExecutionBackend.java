package hiconic.rx.tools.model.configuration;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LocalToolExecutionBackend extends ToolExecutionBackend {

	EntityType<LocalToolExecutionBackend> T = EntityTypes.T(LocalToolExecutionBackend.class);

}
