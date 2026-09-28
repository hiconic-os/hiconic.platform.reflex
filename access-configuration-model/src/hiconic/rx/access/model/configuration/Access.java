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
package hiconic.rx.access.model.configuration;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface Access extends GenericEntity {
	EntityType<Access> T = EntityTypes.T(Access.class);

	@Mandatory
	String getAccessId();
	void setAccessId(String accessId);

	/** Optional human-readable name used by reflective user interfaces. */
	String getDisplayName();
	void setDisplayName(String displayName);

	List<String> getDataModelNames();
	void setDataModelNames(List<String> dataModelNames);
	
	List<String> getServiceModelNames();
	void setServiceModelNames(List<String> serviceModelNames);

	/**
	 * Optional id of the default resource storage used by the resource API of this access.
	 */
	String getResourceStorageId();
	void setResourceStorageId(String resourceStorageId);

	/** Optional id of the access containing this access' workbench model and data. */
}
