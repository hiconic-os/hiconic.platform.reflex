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

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum IdentityManagementMode implements EnumBase {
	
	/**
	 * No identity management done at all. 
	 */
	off, 
	
	/**
	 * Depending on the parsed assembly the marshaler automatically detects the identification information and uses it for identity management.
	 */
	auto, 

	/**
	 * The internal generated _id information will be used if available. 
	 */
	_id,
	

	/**
	 * The id property will be used if available. 
	 */
	id;

	public static final EnumType T = EnumTypes.T(IdentityManagementMode.class);
	
	@Override
	public EnumType type() {
		return T;
	}	
}