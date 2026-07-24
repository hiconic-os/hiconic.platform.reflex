package hiconic.rx.platform.logging;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.logging.Logger;
import com.braintribe.logging.level.persistence.LogLevelPersistence;
import com.braintribe.logging.level.persistence.LogLevelValueResolver;

import hiconic.rx.platform.conf.LayeredConfigurationEntries;
import hiconic.rx.platform.conf.LayeredConfigurationEntries.Entry;

public class LayeredLogLevelPersistence implements LogLevelPersistence {
	private static final Logger log = Logger.getLogger(LayeredLogLevelPersistence.class);

	private final LayeredConfigurationEntries entries;
	private final Function<String, String> propertyLookup;

	public LayeredLogLevelPersistence(ClasspathIndex classpathIndex, String classpathConfPath, File configFolder) {
		this(classpathIndex, classpathConfPath, configFolder, null);
	}

	public LayeredLogLevelPersistence(ClasspathIndex classpathIndex, String classpathConfPath, File configFolder,
			Function<String, String> propertyLookup) {
		entries = new LayeredConfigurationEntries(classpathIndex, classpathConfPath, configFolder, "log-levels", ".properties");
		this.propertyLookup = propertyLookup;
	}

	@Override
	public Map<String, String> getLogLevels() {
		Map<String, String> levels = new LinkedHashMap<>();

		for (Entry entry : entries.entries()) {
			Properties properties = new Properties();

			try {
				if (entry.classpath()) {
					try (InputStreamReader reader = new InputStreamReader(entry.url().openStream(), StandardCharsets.UTF_8)) {
						properties.load(reader);
					}
				} else {
					try (InputStreamReader reader = new InputStreamReader(java.nio.file.Files.newInputStream(entry.file().toPath()), StandardCharsets.UTF_8)) {
						properties.load(reader);
					}
				}
			} catch (IOException e) {
				log.warn("Error while reading packaged log levels from [" + entry.source() + "]", e);
				continue;
			}

			for (String name : properties.stringPropertyNames()) {
				levels.put(name, LogLevelValueResolver.resolveValue(properties.getProperty(name), propertyLookup));
			}
		}

		return levels;
	}

	@Override
	public void updateLogLevels(Map<String, String> levels, Set<String> namesToRemove) {
		throw new UnsupportedOperationException("Packaged log levels are read-only");
	}

	@Override
	public void clearLogLevels() {
		throw new UnsupportedOperationException("Packaged log levels are read-only");
	}
}
