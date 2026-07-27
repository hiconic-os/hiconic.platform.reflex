package hiconic.rx.tools.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface ExternalToolsConfiguration extends GenericEntity {

	EntityType<ExternalToolsConfiguration> T = EntityTypes.T(ExternalToolsConfiguration.class);

	String getFileSystemRoot();
	void setFileSystemRoot(String fileSystemRoot);

	ToolExecutionBackend getBackend();
	void setBackend(ToolExecutionBackend backend);

	List<ExternalToolMapping> getTools();
	void setTools(List<ExternalToolMapping> tools);

}
