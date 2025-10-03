package hiconic.rx.module.api.log;

public interface RxLogManager {
	void setLogLevel(String logger, System.Logger.Level level);
	void setLogLevel(Class<?> clazz, System.Logger.Level level);
}
