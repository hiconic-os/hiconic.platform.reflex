package hiconic.rx.platform.logging;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import hiconic.rx.platform.conf.LayeredConfigurationEntries;
import hiconic.rx.platform.conf.LayeredConfigurationEntries.Entry;

/**
 * Applies indexed classpath and external Logback configurations in a deterministic order.
 * <p>
 * Packaged {@code logback*.xml} entries are applied first, followed by external
 * {@code conf/logback*.xml} entries. All entries participate in the same ordered
 * composition without an implicit reset between configuration layers. If no
 * application configuration exists, the platform's internal file configuration
 * is applied as a fallback.
 */
public class LayeredLogbackConfiguration {
	private static final String BASE_NAME = "logback";
	private static final String EXTENSION = ".xml";
	private static final String DEFAULT_CONFIGURATION = "default-logback.xml";

	private final LayeredConfigurationEntries entries;

	public LayeredLogbackConfiguration(ClasspathIndex classpathIndex, String classpathConfPath, File configFolder) {
		entries = new LayeredConfigurationEntries(classpathIndex, classpathConfPath, configFolder, BASE_NAME, EXTENSION);
	}

	/**
	 * @return {@code true}; either the application configuration or the platform
	 *         fallback is always applied
	 */
	public boolean configure(LoggerContext context) {
		List<Entry> configurationEntries = entries.entries();

		context.reset();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);

		if (configurationEntries.isEmpty())
			configure(configurator, defaultConfiguration());
		else
			for (Entry entry : configurationEntries)
				configure(configurator, entry);

		return true;
	}

	private URL defaultConfiguration() {
		URL resource = LayeredLogbackConfiguration.class.getResource(DEFAULT_CONFIGURATION);
		if (resource == null)
			throw new IllegalStateException("Missing platform Logback fallback [" + DEFAULT_CONFIGURATION + "]");
		return resource;
	}

	private void configure(JoranConfigurator configurator, URL resource) {
		try {
			configurator.doConfigure(resource);
		} catch (JoranException e) {
			throw new IllegalStateException("Error configuring Logback fallback from [" + resource + "]", e);
		}
	}

	private void configure(JoranConfigurator configurator, Entry entry) {
		try {
			if (entry.classpath())
				configurator.doConfigure(entry.url());
			else
				configurator.doConfigure(entry.file());
		} catch (JoranException e) {
			throw new IllegalStateException("Error configuring Logback from [" + entry.source() + "]", e);
		}
	}
}
