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
package hiconic.rx.reflection.model.module;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

@Abstract
public interface ModuleInfo extends GenericEntity {
	
	EntityType<ModuleInfo> T = EntityTypes.T(ModuleInfo.class);
	
	void setName(String name);
	String getName();
	
	void setVersion(String version);
	String getVersion();

	// TODO does model have some info, maybe somehow optionally register what it's doing
//	void setDeployables(List<DeployableInfo> deployables);
//	List<DeployableInfo> getDeployables();
	
}
