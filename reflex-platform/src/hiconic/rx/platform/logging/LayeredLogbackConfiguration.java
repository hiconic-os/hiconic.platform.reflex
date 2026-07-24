package hiconic.rx.platform.logging;

import java.io.File;
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
 * composition without an implicit reset between configuration layers.
 */
public class LayeredLogbackConfiguration {
	private static final String BASE_NAME = "logback";
	private static final String EXTENSION = ".xml";

	private final LayeredConfigurationEntries entries;

	public LayeredLogbackConfiguration(ClasspathIndex classpathIndex, String classpathConfPath, File configFolder) {
		entries = new LayeredConfigurationEntries(classpathIndex, classpathConfPath, configFolder, BASE_NAME, EXTENSION);
	}

	/**
	 * @return {@code true} if at least one explicit configuration was applied
	 */
	public boolean configure(LoggerContext context) {
		List<Entry> configurationEntries = entries.entries();
		if (configurationEntries.isEmpty())
			return false;

		context.reset();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(context);

		for (Entry entry : configurationEntries) {
			configure(configurator, entry);
		}

		return true;
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
