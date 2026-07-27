package hiconic.rx.tools.model.configuration;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface DockerComposeToolExecutionBackend extends ToolExecutionBackend {

	EntityType<DockerComposeToolExecutionBackend> T = EntityTypes.T(DockerComposeToolExecutionBackend.class);

	@Initializer("'docker'")
	String getDockerCommand();
	void setDockerCommand(String dockerCommand);

	String getComposeFile();
	void setComposeFile(String composeFile);

	String getProjectDirectory();
	void setProjectDirectory(String projectDirectory);

	String getService();
	void setService(String service);

	@Initializer("'/tool-fs'")
	String getContainerFileSystemRoot();
	void setContainerFileSystemRoot(String containerFileSystemRoot);

}
