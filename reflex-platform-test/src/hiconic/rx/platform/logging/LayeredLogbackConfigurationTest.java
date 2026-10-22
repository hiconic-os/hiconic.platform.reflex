package hiconic.rx.platform.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.Test;
import org.slf4j.Logger;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.FileAppender;

public class LayeredLogbackConfigurationTest {
	private static final String CLASSPATH_CONF_PATH = "HICONIC-CONF/";

	@Test
	public void appliesInternalFallbackOnlyWithoutApplicationConfiguration() throws Exception {
		Path appDirectory = Files.createTempDirectory("rx-logback-fallback-");
		Path configFolder = appDirectory.resolve("conf");
		Files.createDirectories(configFolder);
		LoggerContext context = new LoggerContext();
		String previousAppDirectory = System.getProperty("reflex.app.dir");

		try {
			System.setProperty("reflex.app.dir", appDirectory.toString());
			boolean configured = new LayeredLogbackConfiguration(new ClasspathIndex(appDirectory), CLASSPATH_CONF_PATH,
					configFolder.toFile()).configure(context);

			assertThat(configured).isTrue();
			var root = context.getLogger(Logger.ROOT_LOGGER_NAME);
			assertThat(root.getLevel()).isEqualTo(Level.INFO);
			assertThat(root.getAppender("FILE")).isInstanceOf(FileAppender.class);
		} finally {
			context.stop();
			if (previousAppDirectory == null)
				System.clearProperty("reflex.app.dir");
			else
				System.setProperty("reflex.app.dir", previousAppDirectory);
			deleteTree(appDirectory);
		}
	}

	@Test
	public void appliesMultipleIndexedClasspathConfigurations() throws Exception {
		Path emptyConfigFolder = Files.createTempDirectory("rx-logback-empty-");
		LoggerContext context = new LoggerContext();

		try {
			boolean configured = configuration(emptyConfigFolder.toFile()).configure(context);

			assertThat(configured).isTrue();
			assertThat(context.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.WARN);
			assertThat(context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("FILE")).isNull();
			assertThat(context.getLogger("hiconic.rx.test.packaged.base").getLevel()).isEqualTo(Level.DEBUG);
			assertThat(context.getLogger("hiconic.rx.test.packaged.extension").getLevel()).isEqualTo(Level.TRACE);
		} finally {
			context.stop();
			deleteTree(emptyConfigFolder);
		}
	}

	@Test
	public void externalConfigurationsContinuePackagedComposition() throws Exception {
		Path configFolder = Files.createTempDirectory("rx-logback-external-");
		LoggerContext context = new LoggerContext();

		try {
			write(configFolder.resolve("logback.xml"), "<configuration>"
					+ "<root level=\"ERROR\"/>"
					+ "<logger name=\"hiconic.rx.test.external.root\" level=\"INFO\"/>"
					+ "</configuration>");
			write(configFolder.resolve("logback.pipeline-20.xml"), "<configuration>"
					+ "<logger name=\"hiconic.rx.test.external.extension\" level=\"DEBUG\"/>"
					+ "</configuration>");

			configuration(configFolder.toFile()).configure(context);

			assertThat(context.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.ERROR);
			assertThat(context.getLogger("hiconic.rx.test.packaged.base").getLevel()).isEqualTo(Level.DEBUG);
			assertThat(context.getLogger("hiconic.rx.test.packaged.extension").getLevel()).isEqualTo(Level.TRACE);
			assertThat(context.getLogger("hiconic.rx.test.external.root").getLevel()).isEqualTo(Level.INFO);
			assertThat(context.getLogger("hiconic.rx.test.external.extension").getLevel()).isEqualTo(Level.DEBUG);
		} finally {
			context.stop();
			deleteTree(configFolder);
		}
	}

	private LayeredLogbackConfiguration configuration(File configFolder) {
		return new LayeredLogbackConfiguration(new ClasspathIndex(getClass().getClassLoader()), CLASSPATH_CONF_PATH, configFolder);
	}

	private void write(Path path, String content) throws Exception {
		Files.write(path, content.getBytes(StandardCharsets.UTF_8));
	}

	private void deleteTree(Path root) throws Exception {
		try (var paths = Files.walk(root)) {
			for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator)
				Files.deleteIfExists(path);
		}
	}
}
