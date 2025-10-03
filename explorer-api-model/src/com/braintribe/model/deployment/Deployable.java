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
package com.braintribe.model.deployment;

import com.braintribe.model.bapi.AvailableAccessesRequest;
import com.braintribe.model.descriptive.HasExternalId;
import com.braintribe.model.descriptive.HasName;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.SelectiveInformation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.HasMetaData;

/**
 * Copied from tribefire.cortex for compatibility with tribefire explorer, which calls {@link AvailableAccessesRequest}. 
 */
@Abstract
@SelectiveInformation("${name} (${externalId})")
public interface Deployable extends HasExternalId, HasName, HasMetaData {

	EntityType<Deployable> T = EntityTypes.T(Deployable.class);

//	String module = "module";
	String autoDeploy = "autoDeploy";
	String deploymentStatus = "deploymentStatus";

//	Module getModule();
//	void setModule(Module module);

	@Initializer("true")
	boolean getAutoDeploy();
	void setAutoDeploy(boolean autoDeploy);

	@Initializer("deployed")
	DeploymentStatus getDeploymentStatus();
	void setDeploymentStatus(DeploymentStatus status);

	default String shortDescription() {
		return entityType().getShortName() + "[" + getExternalId() + "]";
	}

}
