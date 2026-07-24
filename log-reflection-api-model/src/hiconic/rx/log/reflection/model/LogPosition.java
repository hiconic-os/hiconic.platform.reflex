package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * Identifies a record within its stream. Sequence is used by structured live streams, byteOffset by file streams.
 */
public interface LogPosition extends GenericEntity {
	EntityType<LogPosition> T = EntityTypes.T(LogPosition.class);

	Long getSequence();
	void setSequence(Long sequence);

	Long getByteOffset();
	void setByteOffset(Long byteOffset);
}
