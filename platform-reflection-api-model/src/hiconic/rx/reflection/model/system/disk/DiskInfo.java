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
package hiconic.rx.reflection.model.system.disk;

import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface DiskInfo extends Capacity {

	EntityType<DiskInfo> T = EntityTypes.T(DiskInfo.class);

	String getModel();
	void setModel(String model);

	String getDiskName();
	void setDiskName(String diskName);

	String getSerial();
	void setSerial(String serial);

	List<Partition> getPartitions();
	void setPartitions(List<Partition> partitions);

	long getReadBytes();
	void setReadBytes(long readBytes);

	Double getReadBytesInGb();
	void setReadBytesInGb(Double readBytesInGb);

	long getReads();
	void setReads(long reads);

	long getTimeStamp();
	void setTimeStamp(long timeStamp);

	long getTransferTime();
	void setTransferTime(long transferTime);

	long getWriteBytes();
	void setWriteBytes(long writeBytes);

	Double getWriteBytesInGb();
	void setWriteBytesInGb(Double writeBytesInGb);

	long getWrites();
	void setWrites(long writes);

}
