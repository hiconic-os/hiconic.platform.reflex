package hiconic.rx.platform.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import org.junit.Test;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;

public class LayeredLogLevelPersistenceTest {
	@Test
	public void composesClasspathThenFilesystemAndResolvesPlaceholders() throws Exception {
		Path configFolder = Files.createTempDirectory("rx-log-levels-");

		try {
			Files.write(configFolder.resolve("log-levels.properties"), (
					"hiconic.rx.test.packaged=ERROR\n" +
					"hiconic.rx.test.filesystem=INFO\n").getBytes(StandardCharsets.UTF_8));

			LayeredLogLevelPersistence persistence = new LayeredLogLevelPersistence(
					new ClasspathIndex(getClass().getClassLoader()), "HICONIC-CONF/", configFolder.toFile(),
					name -> "test.log.level".equals(name) ? "ERROR" : null);

			Map<String, String> levels = persistence.getLogLevels();

			assertThat(levels).containsEntry("ROOT", "INFO")
					.containsEntry("hiconic.rx.test.packaged", "ERROR")
					.containsEntry("hiconic.rx.test.module", "WARN")
					.containsEntry("hiconic.rx.test.filesystem", "INFO")
					.containsEntry("hiconic.rx.test.placeholder", "ERROR");
		} finally {
			try (var paths = Files.walk(configFolder)) {
				for (Path path : (Iterable<Path>) paths.sorted(Comparator.reverseOrder())::iterator)
					Files.deleteIfExists(path);
			}
		}
	}
}
