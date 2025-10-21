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

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * Models a JSON reference (https://json-spec.readthedocs.io/reference.html). Entities that want to explicitly support
 * being referenced this way (thus supporting respective tooling) can implement this interface.
 *
 * @author Neidhart.Orlich
 *
 */
public interface JsonReferencable extends GenericEntity {

	EntityType<JsonReferencable> T = EntityTypes.T(JsonReferencable.class);

	String get$ref();
	void set$ref(String type);
}
