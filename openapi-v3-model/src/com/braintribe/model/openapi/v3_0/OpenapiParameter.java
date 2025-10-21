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

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * See https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#parameterObject
 */
public interface OpenapiParameter extends OpenapiParameterBase {

	EntityType<OpenapiParameter> T = EntityTypes.T(OpenapiParameter.class);

	@Mandatory
	String getName();
	void setName(String name);

	/**
	 * This should really be an enum, but we want it to be serialized as String so we have no choice.
	 *
	 * Possible values: "query", "header", "path", "cookie"
	 */
	@Mandatory
	String getIn();
	void setIn(String in);

}
