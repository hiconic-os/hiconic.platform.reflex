package hiconic.rx.web.rpc.client.model.config;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.securityservice.credentials.Credentials;

public interface WebRpcClientConnection extends GenericEntity {
	EntityType<WebRpcClientConnection> T = EntityTypes.T(WebRpcClientConnection.class);
	
	String name = "name";
	String url = "url";
	String credentials = "credentials";
	
	@Mandatory
	String getName();
	void setName(String name);
	
	@Mandatory
	String getUrl();
	void setUrl(String url);
	
	Credentials getCredentials();
	void setCredentials(Credentials credentials);
}
