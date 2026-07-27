package hiconic.rx.tools.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ExternalToolMapping extends GenericEntity {

	EntityType<ExternalToolMapping> T = EntityTypes.T(ExternalToolMapping.class);

	String getToolId();
	void setToolId(String toolId);

	String getCommand();
	void setCommand(String command);

	List<String> getFixedArguments();
	void setFixedArguments(List<String> fixedArguments);

}
