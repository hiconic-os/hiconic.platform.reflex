package hiconic.rx.platform.logging;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import com.braintribe.logging.level.LogLevelNames;
import com.braintribe.logging.level.LogLevelFramework;
import com.braintribe.logging.level.StructuredPackageComparator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class LogbackLogLevelFramework implements LogLevelFramework {
	public static final String LEVEL_TRACE = "TRACE";
	public static final String LEVEL_DEBUG = "DEBUG";
	public static final String LEVEL_INFO = "INFO";
	public static final String LEVEL_WARN = "WARN";
	public static final String LEVEL_ERROR = "ERROR";
	public static final String LEVEL_FATAL = "FATAL";

	@Override
	public Map<String, String> getConfiguredLogLevels() {
		Map<String, String> levels = new LinkedHashMap<>();

		for (Logger logger: loggerContext().getLoggerList()) {
			if (logger.getLevel() != null) {
				levels.put(fromLogbackLoggerName(logger.getName()), fromLogbackLevel(logger.getLevel()));
			}
		}

		return levels;
	}

	@Override
	public Set<String> getKnownLoggerNames() {
		Set<String> loggerNames = new TreeSet<>(new StructuredPackageComparator());

		for (Logger logger: loggerContext().getLoggerList()) {
			loggerNames.add(fromLogbackLoggerName(logger.getName()));
		}

		for (Package pkg: Package.getPackages()) {
			loggerNames.add(pkg.getName());
		}

		return loggerNames;
	}

	@Override
	public void applyLogLevels(Map<String, String> levels) {
		if (levels == null || levels.isEmpty()) {
			return;
		}

		for (Map.Entry<String, String> entry: levels.entrySet()) {
			loggerContext().getLogger(toLogbackLoggerName(entry.getKey())).setLevel(toLogbackLevel(entry.getValue()));
		}
	}

	@Override
	public void clearLogLevels(Set<String> loggerNames) {
		if (loggerNames == null || loggerNames.isEmpty()) {
			return;
		}

		for (String loggerName: loggerNames) {
			if (Logger.ROOT_LOGGER_NAME.equals(toLogbackLoggerName(loggerName))) {
				continue;
			}

			loggerContext().getLogger(toLogbackLoggerName(loggerName)).setLevel(null);
		}
	}

	private static String toLogbackLoggerName(String loggerName) {
		return LogLevelNames.ROOT.equals(loggerName) ? Logger.ROOT_LOGGER_NAME : loggerName;
	}

	private static String fromLogbackLoggerName(String loggerName) {
		return Logger.ROOT_LOGGER_NAME.equals(loggerName) ? LogLevelNames.ROOT : loggerName;
	}

	public static Level toLogbackLevel(String levelName) {
		switch (normalize(levelName)) {
			case LEVEL_TRACE:
				return Level.TRACE;
			case LEVEL_DEBUG:
				return Level.DEBUG;
			case LEVEL_INFO:
				return Level.INFO;
			case LEVEL_WARN:
				return Level.WARN;
			case LEVEL_ERROR:
			case LEVEL_FATAL:
				return Level.ERROR;
			default:
				throw new IllegalArgumentException("Unsupported log level: " + levelName);
		}
	}

	public static String fromLogbackLevel(Level logbackLevel) {
		if (logbackLevel == null) {
			return null;
		}

		if (logbackLevel.isGreaterOrEqual(Level.ERROR)) {
			return LEVEL_ERROR;
		}
		if (logbackLevel.isGreaterOrEqual(Level.WARN)) {
			return LEVEL_WARN;
		}
		if (logbackLevel.isGreaterOrEqual(Level.INFO)) {
			return LEVEL_INFO;
		}
		if (logbackLevel.isGreaterOrEqual(Level.DEBUG)) {
			return LEVEL_DEBUG;
		}

		return LEVEL_TRACE;
	}

	private static String normalize(String levelName) {
		if (levelName == null) {
			throw new IllegalArgumentException("Log level must not be null");
		}

		return levelName.trim().toUpperCase(Locale.ROOT);
	}

	private LoggerContext loggerContext() {
		return (LoggerContext) LoggerFactory.getILoggerFactory();
	}
}
