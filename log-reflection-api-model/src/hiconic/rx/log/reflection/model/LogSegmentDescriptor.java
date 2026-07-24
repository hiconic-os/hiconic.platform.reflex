package hiconic.rx.log.reflection.model;

import java.util.Date;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogSegmentDescriptor extends GenericEntity {
	EntityType<LogSegmentDescriptor> T = EntityTypes.T(LogSegmentDescriptor.class);

	String getSegmentId();
	void setSegmentId(String segmentId);

	String getFileName();
	void setFileName(String fileName);

	Long getSize();
	void setSize(Long size);

	Date getLastModified();
	void setLastModified(Date lastModified);

	boolean getActive();
	void setActive(boolean active);
}
