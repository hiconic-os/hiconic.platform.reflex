package hiconic.rx.log.reflection.model;

import java.util.List;
import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogStreamDescriptor extends GenericEntity {
	EntityType<LogStreamDescriptor> T = EntityTypes.T(LogStreamDescriptor.class);

	String getStreamId();
	void setStreamId(String streamId);

	String getDisplayName();
	void setDisplayName(String displayName);

	LogOrigin getOrigin();
	void setOrigin(LogOrigin origin);

	LogStreamKind getKind();
	void setKind(LogStreamKind kind);

	LogFormat getFormat();
	void setFormat(LogFormat format);

	LogParsingQuality getParsingQuality();
	void setParsingQuality(LogParsingQuality parsingQuality);

	String getPattern();
	void setPattern(String pattern);

	Set<LogCapability> getCapabilities();
	void setCapabilities(Set<LogCapability> capabilities);

	Set<String> getCustomFields();
	void setCustomFields(Set<String> customFields);

	List<LogSegmentDescriptor> getSegments();
	void setSegments(List<LogSegmentDescriptor> segments);
}
