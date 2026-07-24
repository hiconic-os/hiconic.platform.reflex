package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogSourceLocation extends GenericEntity {
	EntityType<LogSourceLocation> T = EntityTypes.T(LogSourceLocation.class);

	String getClassName();
	void setClassName(String className);

	String getMethodName();
	void setMethodName(String methodName);

	String getFileName();
	void setFileName(String fileName);

	Integer getLineNumber();
	void setLineNumber(Integer lineNumber);
}
