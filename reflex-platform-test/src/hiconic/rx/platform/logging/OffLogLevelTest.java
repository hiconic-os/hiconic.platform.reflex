package hiconic.rx.platform.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.braintribe.logging.level.JulLogLevelFramework;
import com.braintribe.logging.level.LogLevelConfiguration;

import ch.qos.logback.classic.Level;

public class OffLogLevelTest {
	@Test
	public void supportsOffThroughoutTheLoggingStack() {
		assertThat(LogLevelConfiguration.isSupportedLogLevel("OFF")).isTrue();
		assertThat(LogLevelConfiguration.normalizeLogLevel(" off ")).isEqualTo("OFF");
		assertThat(JulLogLevelFramework.toJulLevel("OFF")).isSameAs(java.util.logging.Level.OFF);
		assertThat(JulLogLevelFramework.fromJulLevel(java.util.logging.Level.OFF)).isEqualTo("OFF");
		assertThat(LogbackLogLevelFramework.toLogbackLevel(" off ")).isSameAs(Level.OFF);
		assertThat(LogbackLogLevelFramework.fromLogbackLevel(Level.OFF)).isEqualTo("OFF");
	}
}
