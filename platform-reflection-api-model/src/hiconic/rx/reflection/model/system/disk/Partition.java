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

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Partition extends GenericEntity {

	EntityType<Partition> T = EntityTypes.T(Partition.class);

	String getIdentification();
	void setIdentification(String identification);

	String getMountPoint();
	void setMountPoint(String mountPoint);

	String getName();
	void setName(String name);

	long getSize();
	void setSize(long size);

	Double getSizeInGb();
	void setSizeInGb(Double sizeInGb);

	String getType();
	void setType(String type);

	String getUuid();
	void setUuid(String uuid);

	int getMajor();
	void setMajor(int major);

	int getMinor();
	void setMinor(int minor);

}
