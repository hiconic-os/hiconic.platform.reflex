package hiconic.rx.log.reflection.model;

import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface LogLevelProfile extends GenericEntity {
	EntityType<LogLevelProfile> T = EntityTypes.T(LogLevelProfile.class);

	String getName();
	void setName(String name);

	Map<String, String> getLevels();
	void setLevels(Map<String, String> levels);
}
