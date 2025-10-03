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

public interface NetworkInterface extends GenericEntity {

	EntityType<NetworkInterface> T = EntityTypes.T(NetworkInterface.class);

	String getDisplayName();
	void setDisplayName(String displayName);

	List<String> getIPv4Addresses();
	void setIPv4Addresses(List<String> iPv4Addresses);

	List<String> getIPv6Addresses();
	void setIPv6Addresses(List<String> iPv6Addresses);

	String getMacAddress();
	void setMacAddress(String macAddress);

	long getMtu();
	void setMtu(long mtu);

	String getName();
	void setName(String name);

	long getSpeed();
	void setSpeed(long speed);

	String getSpeedDisplay();
	void setSpeedDisplay(String speedDisplay);

	long getBytesRecv();
	void setBytesRecv(long bytesRecv);

	Double getBytesRecvInGb();
	void setBytesRecvInGb(Double bytesRecvInGb);

	long getBytesSent();
	void setBytesSent(long bytesSent);

	Double getBytesSentInGb();
	void setBytesSentInGb(Double bytesSentInGb);

	long getPacketsRecv();
	void setPacketsRecv(long packetsRecv);

	long getPacketsSent();
	void setPacketsSent(long packetsSent);

}
