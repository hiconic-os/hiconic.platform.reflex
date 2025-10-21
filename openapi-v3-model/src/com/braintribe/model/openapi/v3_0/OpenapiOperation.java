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
import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * An operation basically describes the api for a certain path/method combination
 * <p>
 * For details see https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#operationObject
 */
public interface OpenapiOperation extends GenericEntity {

	EntityType<OpenapiOperation> T = EntityTypes.T(OpenapiOperation.class);

	String getSummary();
	void setSummary(String summary);

	String getDescription();
	void setDescription(String description);

	/**
	 * The key here is either a code (200, 500, ...) or "default" (which is why it's a string and not an int).
	 */
	@Mandatory
	Map<String, OpenapiResponse> getResponses();
	void setResponses(Map<String, OpenapiResponse> responses);

	List<String> getTags();
	void setTags(List<String> tags);

	List<OpenapiParameter> getParameters();
	void setParameters(List<OpenapiParameter> parameters);

	OpenapiRequestBody getRequestBody();
	void setRequestBody(OpenapiRequestBody reqestBody);

	boolean getDeprecated();
	void setDeprecated(boolean deprecated);

	List<OpenapiServer> getServers();
	void setServers(List<OpenapiServer> servers);

}
