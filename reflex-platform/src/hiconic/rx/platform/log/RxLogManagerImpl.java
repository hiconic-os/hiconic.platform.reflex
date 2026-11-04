package hiconic.rx.platform.log;

import java.lang.System.Logger.Level;

import org.slf4j.Marker;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import com.braintribe.model.processing.service.api.ServiceRequestExecution;
import hiconic.rx.module.api.log.RxLogManager;

public class RxLogManagerImpl implements RxLogManager {
	private static final ThreadLocal<Boolean> ROUTINE_LOGGING_SUPPRESSED = new ThreadLocal<>();

	private final LoggerContext ctx;
	private final RoutineLoggingSuppressionFilter routineLoggingSuppressionFilter = new RoutineLoggingSuppressionFilter();
	private final Object routineLoggingSuppressionMonitor = new Object();
	private int activeRoutineLoggingSuppressionScopes;

	public RxLogManagerImpl() {
		this((LoggerContext) LoggerFactory.getILoggerFactory());
	}

	RxLogManagerImpl(LoggerContext ctx) {
		this.ctx = ctx;
		routineLoggingSuppressionFilter.start();
	}

	@Override
	public void setLogLevel(String loggerName, Level level) {
		ch.qos.logback.classic.Level logbackLevel = toLogbackLevel(level);

	    Logger logger = ctx.getLogger(loggerName);
	    logger.setLevel(logbackLevel);
	}
	
	@Override
	public void setLogLevel(Class<?> clazz, Level level) {
		setLogLevel(clazz.getName(), level);
	}

	@Override
	public void suppressRoutineLogging(ServiceRequestExecution execution) throws Exception {
		enterRoutineLoggingSuppressionScope();
		Boolean previousValue = ROUTINE_LOGGING_SUPPRESSED.get();
		ROUTINE_LOGGING_SUPPRESSED.set(true);
		try {
			execution.execute();
		} finally {
			if (previousValue == null)
				ROUTINE_LOGGING_SUPPRESSED.remove();
			else
				ROUTINE_LOGGING_SUPPRESSED.set(previousValue);
			leaveRoutineLoggingSuppressionScope();
		}
	}
	
	private void enterRoutineLoggingSuppressionScope() {
		synchronized (routineLoggingSuppressionMonitor) {
			if (activeRoutineLoggingSuppressionScopes++ == 0)
				ctx.addTurboFilter(routineLoggingSuppressionFilter);
		}
	}

	private void leaveRoutineLoggingSuppressionScope() {
		synchronized (routineLoggingSuppressionMonitor) {
			if (--activeRoutineLoggingSuppressionScopes == 0)
				ctx.getTurboFilterList().remove(routineLoggingSuppressionFilter);
		}
	}

	public static ch.qos.logback.classic.Level toLogbackLevel(Level level) {
        if (level == null) {
            return null; // Logback: null = inherit
        }

        return switch (level) {
            case ALL     -> ch.qos.logback.classic.Level.ALL;
            case TRACE   -> ch.qos.logback.classic.Level.TRACE;
            case DEBUG   -> ch.qos.logback.classic.Level.DEBUG;
            case INFO    -> ch.qos.logback.classic.Level.INFO;
            case WARNING -> ch.qos.logback.classic.Level.WARN;
            case ERROR   -> ch.qos.logback.classic.Level.ERROR;
            case OFF     -> ch.qos.logback.classic.Level.OFF;
        };
    }

	private static class RoutineLoggingSuppressionFilter extends TurboFilter {
		@Override
		public FilterReply decide(Marker marker, Logger logger, ch.qos.logback.classic.Level level, String format, Object[] params,
				Throwable throwable) {
			if (!Boolean.TRUE.equals(ROUTINE_LOGGING_SUPPRESSED.get()) || level == null)
				return FilterReply.NEUTRAL;

			return level.isGreaterOrEqual(ch.qos.logback.classic.Level.WARN) ? FilterReply.NEUTRAL : FilterReply.DENY;
		}
	}
	

}
