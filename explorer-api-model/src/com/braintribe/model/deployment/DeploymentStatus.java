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
import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * Copied from tribefire.cortex for compatibility with tribefire explorer, which calls {@link AvailableAccessesRequest}. 
 */
public enum DeploymentStatus implements EnumBase<DeploymentStatus> {
	undeployed, 
	deployed,
	partiallyDeployed;

	public static final EnumType<DeploymentStatus> T = EnumTypes.T(DeploymentStatus.class);
	
	@Override
	public EnumType<DeploymentStatus> type() {
		return T;
	}	
}
