package hiconic.rx.web.rpc.client.model.config;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface WebRpcRemoteServiceDomain extends GenericEntity {
	EntityType<WebRpcRemoteServiceDomain> T = EntityTypes.T(WebRpcRemoteServiceDomain.class);
	
	String connection = "connection";
	String domainId = "domainId";
	
	@Mandatory
	WebRpcClientConnection getConnection();
	void setConnection(WebRpcClientConnection connection);

	@Mandatory
	String getDomainId();
	void setDomainId(String domainId);
	
	@Mandatory
	String getModelName();
	void setModelName(String modelName);
}
