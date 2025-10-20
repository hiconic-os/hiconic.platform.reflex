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
package hiconic.rx.webapi.endpoints;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * Note: Probably better remove this class and its usages...
 * Only serviceDomain and typeSignature seem to be actually used and its implemented confusingly 
 */ 
public interface DdraBaseUrlPathParameters extends GenericEntity {
	
	EntityType<DdraBaseUrlPathParameters> T = EntityTypes.T(DdraBaseUrlPathParameters.class);
	
	String getServiceDomain();
	void setServiceDomain(String accessId);
	
	String getTypeSignature();
	void setTypeSignature(String typeSignature);
	
	String getSessionId();
	void setSessionId(String sessionId);
	
	@Initializer("true")
	boolean getIsDomainExplicit();
	void setIsDomainExplicit(boolean isDomainExplicit);
	
	

}
