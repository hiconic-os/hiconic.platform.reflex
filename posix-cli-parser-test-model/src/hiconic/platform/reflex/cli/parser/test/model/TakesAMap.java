package hiconic.platform.reflex.cli.parser.test.model;

import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Alias;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface TakesAMap extends GenericEntity {
	EntityType<TakesAMap> T = EntityTypes.T(TakesAMap.class);
	
	@Alias("m")
	Map<String, String> getMap();
	void setMap(Map<String, String> map);
}
