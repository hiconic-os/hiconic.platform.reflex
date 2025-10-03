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

public interface FileSystemDetailStore extends GenericEntity {

	EntityType<FileSystemDetailStore> T = EntityTypes.T(FileSystemDetailStore.class);

	String getName();
	void setName(String name);

	String getVolume();
	void setVolume(String volume);

	String getMount();
	void setMount(String mount);

	String getDescription();
	void setDescription(String description);

	String getType();
	void setType(String type);

	String getUuid();
	void setUuid(String uuid);

	long getUsableSpace();
	void setUsableSpace(long usableSpace);

	Double getUsableSpaceInGb();
	void setUsableSpaceInGb(Double usableSpaceInGb);

	long getTotalSpace();
	void setTotalSpace(long totalSpace);

	Double getTotalSpaceInGb();
	void setTotalSpaceInGb(Double totalSpaceInGb);

	long getFreeInodes();
	void setFreeInodes(long freeInodes);

	long getTotalInodes();
	void setTotalInodes(long totalInodes);
}
