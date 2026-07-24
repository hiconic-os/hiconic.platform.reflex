package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogPropertyFilter extends GenericEntity {
	EntityType<LogPropertyFilter> T = EntityTypes.T(LogPropertyFilter.class);

	String getName();
	void setName(String name);

	String getValue();
	void setValue(String value);
}
