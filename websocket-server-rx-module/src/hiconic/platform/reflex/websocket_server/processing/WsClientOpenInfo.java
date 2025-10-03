// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
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
