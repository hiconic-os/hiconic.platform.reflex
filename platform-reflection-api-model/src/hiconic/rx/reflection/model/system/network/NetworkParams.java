// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.reflection.model.system.network;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface NetworkParams extends GenericEntity {

	EntityType<NetworkParams> T = EntityTypes.T(NetworkParams.class);

	String getHostName();
	void setHostName(String hostName);

	String getDomainName();
	void setDomainName(String domainName);

	List<String> getDnsServers();
	void setDnsServers(List<String> dnsServers);

	String getIpv4DefaultGateway();
	void setIpv4DefaultGateway(String ipv4DefaultGateway);

	String getIpv6DefaultGateway();
	void setIpv6DefaultGateway(String ipv6DefaultGateway);

}
