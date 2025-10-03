package hiconic.rx.platform.log;

import java.lang.System.Logger.Level;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import hiconic.rx.module.api.log.RxLogManager;

public class RxLogManagerImpl implements RxLogManager {
	private LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();

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
	

}
