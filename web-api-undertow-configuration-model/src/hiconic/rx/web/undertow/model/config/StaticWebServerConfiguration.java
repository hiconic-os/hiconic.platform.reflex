package hiconic.rx.web.undertow.model.config;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface StaticWebServerConfiguration extends GenericEntity {
	EntityType<StaticWebServerConfiguration> T = EntityTypes.T(StaticWebServerConfiguration.class);
	
	String resourceMapppings = "resourceMapppings";

	List<StaticFilesystemResourceMapping> getResourceMappings();
	void setResourceMappings(List<StaticFilesystemResourceMapping> resourceMappings);
}
