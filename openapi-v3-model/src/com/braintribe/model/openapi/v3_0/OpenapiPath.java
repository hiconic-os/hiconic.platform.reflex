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
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * A collection of all {@link OpenapiOperation}s of different HTTP methods for a certain path
 * <p>
 * See https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#pathItemObject
 */
public interface OpenapiPath extends GenericEntity {

	EntityType<OpenapiPath> T = EntityTypes.T(OpenapiPath.class);

	String getSummary();
	void setSummary(String summary);

	String getDescription();
	void setDescription(String description);

	List<OpenapiServer> getServers();
	void setServers(List<OpenapiServer> servers);

	List<OpenapiParameter> getParameters();
	void setParameters(List<OpenapiParameter> parameters);

	OpenapiOperation getGet();
	void setGet(OpenapiOperation get);

	OpenapiOperation getPost();
	void setPost(OpenapiOperation post);

	OpenapiOperation getPut();
	void setPut(OpenapiOperation put);

	OpenapiOperation getDelete();
	void setDelete(OpenapiOperation delete);

	OpenapiOperation getOptions();
	void setOptions(OpenapiOperation options);

	OpenapiOperation getHead();
	void setHead(OpenapiOperation head);

	OpenapiOperation getPatch();
	void setPatch(OpenapiOperation patch);

	OpenapiOperation getTrace();
	void setTrace(OpenapiOperation trace);

}
