package hiconic.rx.log.reflection.model.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.log.reflection.model.LogRecord;

public interface LogRecordPage extends GenericEntity {
	EntityType<LogRecordPage> T = EntityTypes.T(LogRecordPage.class);

	List<LogRecord> getRecords();
	void setRecords(List<LogRecord> records);

	String getNextCursor();
	void setNextCursor(String nextCursor);

	boolean getMoreAvailable();
	void setMoreAvailable(boolean moreAvailable);

	Set<String> getObservedProperties();
	void setObservedProperties(Set<String> observedProperties);

	/** Per-instance errors for partial cluster responses, keyed by {@code applicationId@nodeId}. */
	Map<String, String> getErrors();
	void setErrors(Map<String, String> errors);
}
