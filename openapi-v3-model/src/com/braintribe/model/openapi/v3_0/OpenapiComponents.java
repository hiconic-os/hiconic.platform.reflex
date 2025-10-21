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

import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * A pool for {@link JsonReferencable} OpenAPI entities that can be reused by referencing them with a json reference
 * <p>
 * See https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#componentsObject
 */
public interface OpenapiComponents extends GenericEntity {

	EntityType<OpenapiComponents> T = EntityTypes.T(OpenapiComponents.class);

	Map<String, OpenapiSchema> getSchemas();
	void setSchemas(Map<String, OpenapiSchema> schemas);

	Map<String, OpenapiResponse> getResponses();
	void setResponses(Map<String, OpenapiResponse> responses);

	Map<String, OpenapiParameter> getParameters();
	void setParameters(Map<String, OpenapiParameter> parameters);

	// Map<String, OpenapiExample> getExamples();
	// void setExamples(Map<String, OpenapiExample> examples);
	//
	Map<String, OpenapiRequestBody> getRequestBodies();
	void setRequestBodies(Map<String, OpenapiRequestBody> requestBodies);

	Map<String, OpenapiHeader> getHeaders();
	void setHeaders(Map<String, OpenapiHeader> headers);

}
