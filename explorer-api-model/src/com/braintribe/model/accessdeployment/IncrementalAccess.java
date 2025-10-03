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
package com.braintribe.model.accessdeployment;

//import com.braintribe.model.accessdeployment.aspect.AspectConfiguration;
import com.braintribe.model.deployment.Deployable;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.service.domain.ServiceDomain;


@Abstract
public interface IncrementalAccess extends Deployable, ServiceDomain {

	EntityType<IncrementalAccess> T = EntityTypes.T(IncrementalAccess.class);
	
	/**
	 * @deprecated This property is not used as of tribefire 2.0 and will be soon removed.
	 */
	@Deprecated
	String getResourceFolderPath();
	@Deprecated
	void setResourceFolderPath(String resourceFolderPath);
	
//	public AspectConfiguration getAspectConfiguration();
//	public void setAspectConfiguration( AspectConfiguration aspectConfiguration);
	
	GmMetaModel getMetaModel();
	void setMetaModel(GmMetaModel metaModel);

	IncrementalAccess getWorkbenchAccess();
	void setWorkbenchAccess(IncrementalAccess workbenchAccess);

	/**
	 * If set to true, the actual type of the access will be ignored during deployment and a {@code SmoodAccess} will be
	 * deployed instead (as a proxy).
	 */
	boolean getSimulated();
	void setSimulated(boolean simulated);

}
