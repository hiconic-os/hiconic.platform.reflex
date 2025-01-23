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
package hiconic.rx.web.server.model.config;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Confidential;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface WebServerConfiguration extends GenericEntity {
	EntityType<WebServerConfiguration> T = EntityTypes.T(WebServerConfiguration.class);
	
	String port = "port";
	String sslPort = "sslPort";
	String hostName = "hostName";
	String sslKeyStore = "sslKeyStore";
	String sslKeyStorePassword = "sslKeyStorePassword";
	
	@Initializer("'localhost'")
	String getHostName();
	void setHostName(String hostName);
	
	@Initializer("8080")
	int getPort();
	void setPort(int port);
	
	Integer getSslPort();
	void setSslPort(Integer sslPort);
	
	String getSslKeyStore();
	void setSslKeyStore(String sslKeyStore);
	
	@Confidential
	String getSslKeyStorePassword();
	void setSslKeyStorePassword(String sslKeyStorePassword);
	
	CorsConfiguration getCorsConfiguration();
	void setCorsConfiguration(CorsConfiguration corsConfiguration);
}
