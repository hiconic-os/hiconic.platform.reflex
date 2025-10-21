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
package com.braintribe.model.openapi.v3_0;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#serverVariableObject
 *
 * @see OpenapiServer
 */
public interface OpenapiServerVariable extends GenericEntity {
	EntityType<OpenapiServerVariable> T = EntityTypes.T(OpenapiServerVariable.class);

	@Mandatory
	String getDefault();
	void setDefault(String defaultValue);

	@Mandatory
	List<String> getEnum();
	void setEnum(List<String> enumeration);

	String getDescription();
	void setDescription(String description);
}
