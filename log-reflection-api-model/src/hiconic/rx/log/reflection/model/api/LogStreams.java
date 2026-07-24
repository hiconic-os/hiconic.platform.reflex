package hiconic.rx.log.reflection.model.api;

import java.util.List;
import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.log.reflection.model.LogStreamDescriptor;

public interface LogStreams extends GenericEntity {
	EntityType<LogStreams> T = EntityTypes.T(LogStreams.class);

	List<LogStreamDescriptor> getStreams();
	void setStreams(List<LogStreamDescriptor> streams);

	/** Per-instance errors for partial cluster responses, keyed by {@code applicationId@nodeId}. */
	Map<String, String> getErrors();
	void setErrors(Map<String, String> errors);
}
