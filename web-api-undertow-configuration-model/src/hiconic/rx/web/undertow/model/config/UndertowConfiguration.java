package hiconic.rx.web.undertow.model.config;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface UndertowConfiguration extends GenericEntity {
	EntityType<UndertowConfiguration> T = EntityTypes.T(UndertowConfiguration.class);
	
	String port = "port";
	
	@Initializer("8080")
	int getPort();
	void setPort(int port);
}
