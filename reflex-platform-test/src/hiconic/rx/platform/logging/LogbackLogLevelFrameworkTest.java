package hiconic.rx.platform.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.braintribe.logging.Logger;
import com.braintribe.logging.level.JulLogLevelFramework;
import com.braintribe.logging.level.LogLevelNames;

import ch.qos.logback.classic.LoggerContext;

public class LogbackLogLevelFrameworkTest {
	private final Map<String, Level> previousJulLevels = new LinkedHashMap<>();

	@Before
	public void rememberJulLevels() {
		LogManager logManager = LogManager.getLogManager();
		logManager.getLoggerNames().asIterator().forEachRemaining(loggerName -> {
			java.util.logging.Logger logger = logManager.getLogger(loggerName);
			if (logger != null) {
				previousJulLevels.put(loggerName, logger.getLevel());
			}
		});
	}

	@After
	public void restoreJulLevels() {
		LogManager logManager = LogManager.getLogManager();
		logManager.getLoggerNames().asIterator().forEachRemaining(loggerName -> {
			java.util.logging.Logger logger = logManager.getLogger(loggerName);
			if (logger != null) {
				logger.setLevel(previousJulLevels.get(loggerName));
			}
		});
	}

	@Test
	public void synchronizesInheritedDebugLevelIntoJulGate() {
		String parentName = getClass().getName() + ".managed";
		String childName = parentName + ".Processor";
		java.util.logging.Logger julRoot = java.util.logging.Logger.getLogger("");
		java.util.logging.Logger julParent = java.util.logging.Logger.getLogger(parentName);
		java.util.logging.Logger julChild = java.util.logging.Logger.getLogger(childName);
		LoggerContext logback = new LoggerContext();

		try {
			julChild.setLevel(Level.INFO); // A stale JUL level would discard DEBUG before it reaches Logback.
			LogbackLogLevelFramework framework = new LogbackLogLevelFramework(logback, new JulLogLevelFramework());

			framework.clearLogLevels(Set.of(parentName));
			framework.applyLogLevels(Map.of(LogLevelNames.ROOT, LogbackLogLevelFramework.LEVEL_INFO,
					parentName, LogbackLogLevelFramework.LEVEL_DEBUG));

			assertThat(logback.getLogger(childName).getEffectiveLevel()).isEqualTo(ch.qos.logback.classic.Level.DEBUG);
			assertThat(julRoot.getLevel()).isEqualTo(Level.INFO);
			assertThat(julParent.getLevel()).isEqualTo(Level.FINE);
			assertThat(julChild.getLevel()).isNull();
			assertThat(julChild.isLoggable(Level.FINE)).isTrue();
			assertThat(Logger.getLogger(childName, getClass()).isDebugEnabled()).isTrue();
		} finally {
			logback.stop();
		}
	}

	@Test
	public void removingOverrideRestoresSynchronizedRootGateAndSupplierLaziness() {
		String loggerName = getClass().getName() + ".removed";
		java.util.logging.Logger julRoot = java.util.logging.Logger.getLogger("");
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(loggerName);
		LoggerContext logback = new LoggerContext();

		try {
			LogbackLogLevelFramework framework = new LogbackLogLevelFramework(logback, new JulLogLevelFramework());
			framework.applyLogLevels(Map.of(LogLevelNames.ROOT, LogbackLogLevelFramework.LEVEL_INFO,
					loggerName, LogbackLogLevelFramework.LEVEL_DEBUG));

			framework.clearLogLevels(Set.of(loggerName));
			framework.applyLogLevels(Map.of(LogLevelNames.ROOT, LogbackLogLevelFramework.LEVEL_INFO));

			assertThat(logback.getLogger(loggerName).getLevel()).isNull();
			assertThat(logback.getLogger(loggerName).getEffectiveLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
			assertThat(julRoot.getLevel()).isEqualTo(Level.INFO);
			assertThat(julLogger.getLevel()).isNull();
			assertThat(julLogger.isLoggable(Level.FINE)).isFalse();

			int[] supplierCalls = { 0 };
			Logger.getLogger(loggerName, getClass()).debug(() -> {
				supplierCalls[0]++;
				return "must stay lazy";
			});
			assertThat(supplierCalls[0]).isZero();
		} finally {
			logback.stop();
		}
	}
}
