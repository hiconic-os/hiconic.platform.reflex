package hiconic.rx.platform.configuration;

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;

import ch.qos.logback.classic.LoggerContext;
import hiconic.rx.platform.logging.LayeredLogLevelPersistence;
import hiconic.rx.platform.logging.LayeredLogbackConfiguration;

public class WellKnownConfigurationCompilerTest {
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void compiledLogLevelsRetainLayeringAndRuntimePlaceholderResolution() throws Exception {
		Path resources = temporaryFolder.newFolder("log-level-resources").toPath();
		artifact(resources, "artifact-a", Map.of(
				"HICONIC-CONF/log-levels.properties", "ROOT=INFO\nshared=DEBUG\nplaceholder=${LEVEL}\n"));
		artifact(resources, "artifact-b", Map.of(
				"HICONIC-CONF/log-levels.extension-10.properties", "shared=TRACE\npackaged=INFO\n"));
		ClasspathIndex sourceIndex = new ClasspathIndex(resources);

		Path originalConf = temporaryFolder.newFolder("original-log-level-conf").toPath();
		Files.writeString(originalConf.resolve("log-levels.properties"), "shared=ERROR\nfilesystem=INFO\n");
		Path compiledConf = temporaryFolder.newFolder("compiled-log-level-conf").toPath();
		Files.writeString(compiledConf.resolve("log-levels.properties"), "shared=ERROR\nfilesystem=INFO\n");

		Map<String, String> expected = levels(sourceIndex, originalConf);
		WellKnownConfigurationCompiler.compile(sourceIndex, compiledConf);
		Map<String, String> actual = levels(new ClasspathIndex(), compiledConf);

		assertThat(actual).isEqualTo(expected)
				.containsEntry("shared", "ERROR")
				.containsEntry("placeholder", "WARN");
		assertThat(Files.list(compiledConf)).extracting(path -> path.getFileName().toString())
				.containsExactly("log-levels.properties");
		assertThat(Files.readString(compiledConf.resolve("log-levels.properties"))).contains("placeholder=${LEVEL}");
	}

	@Test
	public void compiledLogbackHasTheSameEffectiveLoggerStateAsSequentialJoranLayers() throws Exception {
		Path resources = temporaryFolder.newFolder("logback-resources").toPath();
		artifact(resources, "artifact-a", Map.of(
				"HICONIC-CONF/logback.xml", "<configuration><root level=\"WARN\"/><logger name=\"from.a\" level=\"DEBUG\"/></configuration>"));
		artifact(resources, "artifact-b", Map.of(
				"HICONIC-CONF/logback.extension-10.xml", "<configuration><logger name=\"from.b\" level=\"TRACE\"/></configuration>"));
		ClasspathIndex sourceIndex = new ClasspathIndex(resources);

		Path originalConf = temporaryFolder.newFolder("original-logback-conf").toPath();
		writeLogbackPipelineLayers(originalConf);
		Path compiledConf = temporaryFolder.newFolder("compiled-logback-conf").toPath();
		writeLogbackPipelineLayers(compiledConf);

		LoggerContext sequential = configure(sourceIndex, originalConf);
		WellKnownConfigurationCompiler.compile(sourceIndex, compiledConf);
		LoggerContext compiled = configure(new ClasspathIndex(), compiledConf);
		try {
			assertThat(level(compiled, Logger.ROOT_LOGGER_NAME)).isEqualTo(level(sequential, Logger.ROOT_LOGGER_NAME));
			for (String logger : new String[] { "from.a", "from.b", "from.pipeline.base", "from.pipeline.extension" })
				assertThat(level(compiled, logger)).as(logger).isEqualTo(level(sequential, logger));
			assertThat(Files.list(compiledConf)).extracting(path -> path.getFileName().toString())
					.containsExactly("logback.xml");
		} finally {
			sequential.stop();
			compiled.stop();
		}
	}

	private Map<String, String> levels(ClasspathIndex index, Path conf) {
		return new LayeredLogLevelPersistence(index, "HICONIC-CONF/", conf.toFile(),
				name -> "LEVEL".equals(name) ? "WARN" : null).getLogLevels();
	}

	private LoggerContext configure(ClasspathIndex index, Path conf) {
		LoggerContext context = new LoggerContext();
		new LayeredLogbackConfiguration(index, "HICONIC-CONF/", conf.toFile()).configure(context);
		return context;
	}

	private String level(LoggerContext context, String logger) {
		return String.valueOf(context.getLogger(logger).getLevel());
	}

	private void writeLogbackPipelineLayers(Path conf) throws Exception {
		Files.writeString(conf.resolve("logback.xml"),
				"<configuration><root level=\"ERROR\"/><logger name=\"from.pipeline.base\" level=\"INFO\"/></configuration>");
		Files.writeString(conf.resolve("logback.pipeline-20.xml"),
				"<configuration><logger name=\"from.pipeline.extension\" level=\"DEBUG\"/></configuration>");
	}

	private static void artifact(Path root, String artifact, Map<String, String> resources) throws Exception {
		Path artifactRoot = root.resolve(artifact);
		Path metaInf = artifactRoot.resolve("META-INF");
		Files.createDirectories(metaInf);
		Files.writeString(metaInf.resolve("classpath-index.txt"),
				String.join("\n", resources.keySet().stream().sorted().toList()) + "\n", StandardCharsets.UTF_8);
		Files.writeString(metaInf.resolve("classpath-origin.properties"), "artifactId=" + artifact + "\n", StandardCharsets.UTF_8);
		for (Map.Entry<String, String> resource : resources.entrySet()) {
			Path file = artifactRoot.resolve(resource.getKey());
			Files.createDirectories(file.getParent());
			Files.writeString(file, resource.getValue(), StandardCharsets.UTF_8);
		}
	}
}
