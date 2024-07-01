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
