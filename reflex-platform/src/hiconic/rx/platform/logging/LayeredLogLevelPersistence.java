package hiconic.rx.platform.logging;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.logging.Logger;
import com.braintribe.logging.level.LogLevelProfileNames;
import com.braintribe.logging.level.persistence.LogLevelIniCodec;
import com.braintribe.logging.level.persistence.LogLevelProfilePersistence;
import com.braintribe.logging.level.persistence.LogLevelValueResolver;

import hiconic.rx.platform.conf.LayeredConfigurationEntries;
import hiconic.rx.platform.conf.LayeredConfigurationEntries.Entry;

public class LayeredLogLevelPersistence implements LogLevelProfilePersistence {
	private static final Logger log = Logger.getLogger(LayeredLogLevelPersistence.class);

	private final LayeredConfigurationEntries entries;
	private final LayeredConfigurationEntries profileEntries;
	private final Function<String, String> propertyLookup;

	public LayeredLogLevelPersistence(ClasspathIndex classpathIndex, String classpathConfPath, File configFolder) {
		this(classpathIndex, classpathConfPath, configFolder, null);
	}

	public LayeredLogLevelPersistence(ClasspathIndex classpathIndex, String classpathConfPath, File configFolder,
			Function<String, String> propertyLookup) {
		entries = new LayeredConfigurationEntries(classpathIndex, classpathConfPath, configFolder, "log-levels", ".properties");
		profileEntries = new LayeredConfigurationEntries(classpathIndex, classpathConfPath, configFolder, "log-levels", ".ini");
		this.propertyLookup = propertyLookup;
	}

	@Override
	public Map<String, Map<String, String>> getProfiles() {
		Map<String, Map<String, String>> profiles = new LinkedHashMap<>();
		profiles.put(LogLevelProfileNames.PACKAGED_DEFAULT, getLegacyLogLevels());

		for (Entry entry : profileEntries.entries()) {
			try (Reader reader = reader(entry)) {
				for (Map.Entry<String, Map<String, String>> profile : LogLevelIniCodec.read(reader).entrySet()) {
					Map<String, String> target = profiles.computeIfAbsent(profile.getKey(), key -> new LinkedHashMap<>());
					for (Map.Entry<String, String> level : profile.getValue().entrySet())
						target.put(level.getKey(), LogLevelValueResolver.resolveValue(level.getValue(), propertyLookup));
				}
			} catch (IOException e) {
				log.warn("Error while reading packaged log-level profiles from [" + entry.source() + "]", e);
			}
		}

		return profiles;
	}

	@Override
	public List<String> getProfileCascade() {
		return Collections.singletonList(LogLevelProfileNames.PACKAGED_DEFAULT);
	}

	@Override
	public void updateProfileLogLevels(String profile, Map<String, String> levels, Set<String> namesToRemove) {
		throw new UnsupportedOperationException("Packaged log-level profiles are read-only");
	}

	@Override
	public void clearProfileLogLevels(String profile) {
		throw new UnsupportedOperationException("Packaged log-level profiles are read-only");
	}

	private Reader reader(Entry entry) throws IOException {
		return entry.classpath()
				? new InputStreamReader(entry.url().openStream(), StandardCharsets.UTF_8)
				: new InputStreamReader(java.nio.file.Files.newInputStream(entry.file().toPath()), StandardCharsets.UTF_8);
	}

	@Override
	public Map<String, String> getLogLevels() {
		Map<String, String> levels = getProfiles().get(LogLevelProfileNames.PACKAGED_DEFAULT);
		return levels == null ? new LinkedHashMap<>() : levels;
	}

	private Map<String, String> getLegacyLogLevels() {
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
