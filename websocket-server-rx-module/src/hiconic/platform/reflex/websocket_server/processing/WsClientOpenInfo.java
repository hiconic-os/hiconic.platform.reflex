package hiconic.platform.reflex.websocket_server.processing;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface WsClientOpenInfo extends GenericEntity {
	
	EntityType<WsClientOpenInfo> T = EntityTypes.T(WsClientOpenInfo.class);

	void setSessionId (String sessionId);
	String getSessionId();
	
	void setClientId(String clientId);
	String getClientId();
	
	void setAccept(String accept);
	String getAccept();
	
	boolean getSendChannelId();
	void setSendChannelId(boolean sendChannelId);
}
