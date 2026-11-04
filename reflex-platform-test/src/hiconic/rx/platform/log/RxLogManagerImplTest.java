package hiconic.rx.platform.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class RxLogManagerImplTest {

	private LoggerContext context;
	private RxLogManagerImpl manager;
	private Logger logger;
	private ListAppender<ILoggingEvent> appender;

	@Before
	public void setup() {
		context = new LoggerContext();
		context.start();
		manager = new RxLogManagerImpl(context);
		logger = context.getLogger(getClass());
		logger.setLevel(Level.TRACE);
		appender = new ListAppender<>();
		appender.setContext(context);
		appender.start();
		logger.addAppender(appender);
	}

	@After
	public void cleanup() {
		context.stop();
	}

	@Test
	public void suppressesRoutineLevelsButPreservesWarningsAndErrors() throws Exception {
		manager.suppressRoutineLogging(() -> {
			logger.trace("trace");
			logger.debug("debug");
			logger.info("info");
			logger.warn("warn");
			logger.error("error");
		});

		logger.info("outside");

		assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly("warn", "error", "outside");
	}

	@Test
	public void nestedScopesAndExceptionsRestoreLogging() throws Exception {
		assertThatThrownBy(() -> manager.suppressRoutineLogging(() -> {
			manager.suppressRoutineLogging(() -> logger.info("nested"));
			throw new IllegalStateException("expected");
		})).isInstanceOf(IllegalStateException.class).hasMessage("expected");

		logger.info("restored");

		assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly("restored");
	}
}
