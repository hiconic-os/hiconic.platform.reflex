package hiconic.rx.web.server.model.config;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface WebServerConfiguration extends GenericEntity {
	EntityType<WebServerConfiguration> T = EntityTypes.T(WebServerConfiguration.class);
	
	String port = "port";
	String hostName = "hostName";
	
	@Initializer("'localhost'")
	String getHostName();
	void setHostName(String hostName);
	
	@Initializer("8080")
	int getPort();
	void setPort(int port);
}
