package hiconic.rx.tools.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.braintribe.utils.system.exec.CommandExecution;
import com.braintribe.utils.system.exec.CommandExecutionImpl;
import com.braintribe.utils.system.exec.ProcessTerminatorImpl;
import com.braintribe.utils.system.exec.tool.ExternalToolRegistry;
import com.braintribe.utils.system.exec.tool.StandardToolExecutionEnvironment;
import com.braintribe.utils.system.exec.tool.ToolCommandLineBuilder;
import com.braintribe.utils.system.exec.tool.ToolExecutionEnvironment;
import com.braintribe.utils.system.exec.tool.ToolWorkspace;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.tools.api.ExternalToolsContract;
import hiconic.rx.tools.model.configuration.DockerComposeToolExecutionBackend;
import hiconic.rx.tools.model.configuration.ExternalToolMapping;
import hiconic.rx.tools.model.configuration.ExternalToolsConfiguration;
import hiconic.rx.tools.model.configuration.LocalToolExecutionBackend;
import hiconic.rx.tools.model.configuration.ToolExecutionBackend;

@Managed
public class ExternalToolsRxModuleSpace implements RxModuleContract, ExternalToolsContract {

	@Import
	private RxPlatformContract platform;

	@Override
	public ExternalToolRegistry tools() {
		return toolEnvironment();
	}

	@Override
	public ToolExecutionEnvironment executionEnvironment() {
		return toolEnvironment();
	}

	@Managed
	private StandardToolExecutionEnvironment toolEnvironment() {
		ExternalToolsConfiguration configuration = configuration();
		ToolExecutionBackend backend = configuration.getBackend();
		String backendName = backend instanceof DockerComposeToolExecutionBackend ? "docker-compose" : "local";
		Path root = Paths.get(requireText(configuration.getFileSystemRoot(), "fileSystemRoot"));

		StandardToolExecutionEnvironment environment = new StandardToolExecutionEnvironment(commandExecution(), root, backendName);
		for (ExternalToolMapping mapping : safe(configuration.getTools()))
			register(environment, root, backend, mapping);
		return environment;
	}

	private void register(StandardToolExecutionEnvironment environment, Path root, ToolExecutionBackend backend, ExternalToolMapping mapping) {
		String id = requireText(mapping.getToolId(), "tools[].toolId");
		String command = requireText(mapping.getCommand(), "tools[" + id + "].command");
		List<String> fixedArguments = new ArrayList<String>(safe(mapping.getFixedArguments()));

		if (backend instanceof DockerComposeToolExecutionBackend) {
			DockerComposeToolExecutionBackend docker = (DockerComposeToolExecutionBackend) backend;
			environment.register(id, singleton(command), dockerCommandLine(docker, root, command, fixedArguments));
		} else if (backend == null || backend instanceof LocalToolExecutionBackend) {
			List<String> commandParts = new ArrayList<String>(1 + fixedArguments.size());
			commandParts.add(command);
			commandParts.addAll(fixedArguments);
			environment.register(id, constant(commandParts));
		} else {
			throw new IllegalStateException("Unsupported external tool backend: " + backend.entityType().getTypeSignature());
		}
	}

	private ToolCommandLineBuilder dockerCommandLine(DockerComposeToolExecutionBackend backend, Path hostRoot, String command,
			List<String> fixedArguments) {
		String dockerCommand = requireText(backend.getDockerCommand(), "backend.dockerCommand");
		String service = requireText(backend.getService(), "backend.service");
		Path normalizedHostRoot = hostRoot.toAbsolutePath().normalize();
		Path containerRoot = Paths.get(requireText(backend.getContainerFileSystemRoot(), "backend.containerFileSystemRoot")).normalize();
		if (!containerRoot.isAbsolute())
			throw new IllegalStateException("External tools configuration property 'backend.containerFileSystemRoot' must be absolute.");
		String composeFile = trimToNull(backend.getComposeFile());
		String projectDirectory = trimToNull(backend.getProjectDirectory());

		return (ToolWorkspace workspace, List<String> arguments) -> {
			Path containerWorkspace = containerRoot.resolve(normalizedHostRoot.relativize(workspace.root().toAbsolutePath().normalize())).normalize();
			List<String> result = new ArrayList<String>();
			result.add(dockerCommand);
			result.add("compose");
			if (composeFile != null) {
				result.add("--file");
				result.add(composeFile);
			}
			if (projectDirectory != null) {
				result.add("--project-directory");
				result.add(projectDirectory);
			}
			result.add("exec");
			result.add("-T");
			result.add("--workdir");
			result.add(containerWorkspace.toString());
			result.add(service);
			result.add(command);
			for (String argument : fixedArguments)
				result.add(projectPath(argument, normalizedHostRoot, containerRoot));
			for (String argument : arguments)
				result.add(projectPath(argument, normalizedHostRoot, containerRoot));
			return result;
		};
	}

	private String projectPath(String argument, Path hostRoot, Path containerRoot) {
		if (argument == null)
			return null;
		return argument.replace(hostRoot.toString(), containerRoot.toString());
	}

	@Managed
	private CommandExecution commandExecution() {
		CommandExecutionImpl bean = new CommandExecutionImpl();
		bean.setProcessTerminator(new ProcessTerminatorImpl());
		return bean;
	}

	@Managed
	private ExternalToolsConfiguration configuration() {
		return getOrTunnel(platform.configuration().readConfig(ExternalToolsConfiguration.T));
	}

	private <T> List<T> safe(List<T> values) {
		return values == null ? Collections.<T>emptyList() : values;
	}

	private Supplier<List<String>> singleton(String value) {
		return constant(Collections.singletonList(value));
	}

	private Supplier<List<String>> constant(List<String> values) {
		List<String> copy = Collections.unmodifiableList(new ArrayList<String>(values));
		return () -> copy;
	}

	private String requireText(String value, String property) {
		String result = trimToNull(value);
		if (result == null)
			throw new IllegalStateException("External tools configuration property '" + property + "' must not be blank.");
		return result;
	}

	private String trimToNull(String value) {
		if (value == null)
			return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

}
