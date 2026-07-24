package hiconic.rx.log.reflection.model;

import java.util.Date;
import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.logging.LogLevel;

public interface LogRecord extends GenericEntity {
	EntityType<LogRecord> T = EntityTypes.T(LogRecord.class);

	Date getTimestamp();
	void setTimestamp(Date timestamp);

	LogLevel getLevel();
	void setLevel(LogLevel level);

	LogOrigin getOrigin();
	void setOrigin(LogOrigin origin);

	String getStreamId();
	void setStreamId(String streamId);

	String getLoggerName();
	void setLoggerName(String loggerName);

	String getThreadName();
	void setThreadName(String threadName);

	String getMessage();
	void setMessage(String message);

	String getThrowable();
	void setThrowable(String throwable);

	LogSourceLocation getSourceLocation();
	void setSourceLocation(LogSourceLocation sourceLocation);

	Map<String, String> getProperties();
	void setProperties(Map<String, String> properties);

	String getRawText();
	void setRawText(String rawText);

	LogPosition getPosition();
	void setPosition(LogPosition position);
}
