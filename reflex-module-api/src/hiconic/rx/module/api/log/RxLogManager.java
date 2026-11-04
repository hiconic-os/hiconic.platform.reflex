package hiconic.rx.module.api.log;

import com.braintribe.model.processing.service.api.ServiceRequestExecution;

public interface RxLogManager {
	void setLogLevel(String logger, System.Logger.Level level);
	void setLogLevel(Class<?> clazz, System.Logger.Level level);

	/**
	 * Executes a request scope without routine TRACE, DEBUG and INFO output. WARN and ERROR output is deliberately preserved.
	 */
	void suppressRoutineLogging(ServiceRequestExecution execution) throws Exception;
}
