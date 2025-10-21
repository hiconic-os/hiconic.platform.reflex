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
 * The outer-most entity that corresponds to the Swagger representation of a complete API. This entity may be marshalled
 * to produce a swagger JSON/YAML compatible with the swagger 2.0 spec.
 * <p>
 * See https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.3.md#oasObject
 */
public interface OpenApi extends GenericEntity {

	EntityType<OpenApi> T = EntityTypes.T(OpenApi.class);

	@Mandatory
	String getOpenapi();
	void setOpenapi(String openapiVersion);

	@Mandatory
	OpenapiInfo getInfo();
	void setInfo(OpenapiInfo info);

	OpenapiComponents getComponents();
	void setComponents(OpenapiComponents info);

	@Mandatory
	Map<String, OpenapiPath> getPaths();
	void setPaths(Map<String, OpenapiPath> paths);

	/* List<SwaggerPath> getPathList(); void setPathList(List<SwaggerPath> paths);
	 * 
	 * String getHost(); void setHost(String basePath);
	 * 
	 * String getBasePath(); void setBasePath(String basePath);
	 * 
	 * Map<String, SwaggerSchema> getDefinitions(); void setDefinitions(Map<String, SwaggerSchema> definitions); */
	List<OpenapiTag> getTags();
	void setTags(List<OpenapiTag> tags);

	List<OpenapiServer> getServers();
	void setServers(List<OpenapiServer> servers);

	boolean getUseFullyQualifiedDefinitionName();
	void setUseFullyQualifiedDefinitionName(boolean useFullyQualifiedDefinitionName);

}
