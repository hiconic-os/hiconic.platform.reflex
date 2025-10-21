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
package com.braintribe.model.openapi.v3_0.api;

import java.util.Set;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.openapi.v3_0.OpenApi;
import com.braintribe.model.service.api.AuthorizedRequest;
import com.braintribe.model.service.api.ServiceRequest;

@Abstract
public interface OpenapiRequest extends AuthorizedRequest {

	EntityType<OpenapiRequest> T = EntityTypes.T(OpenapiRequest.class);

	String getServerUrl();
	void setServerUrl(String serverUrl);

	Set<String> getUseCases();
	void setUseCases(Set<String> useCases);

	// TODO delete if not needed
//	GmMetaModel getModel();
//	void setModel(GmMetaModel model);

	@Initializer("true")
	boolean getUseFullyQualifiedDefinitionName();
	void setUseFullyQualifiedDefinitionName(boolean useFullyQualifiedDefinitionName);

	@Initializer("false")
	boolean getUseJSONForExport();
	void setUseJSONForExport(boolean value);
	
	boolean getIncludeSessionId();
	void setIncludeSessionId(boolean includeSessionId);
	
	boolean getReflectSubtypes();
	void setReflectSubtypes(boolean reflectSubtypes);
	
	boolean getReflectSupertypes();
	void setReflectSupertypes(boolean reflectSupertypes);

	@Override
	EvalContext<? extends OpenApi> eval(Evaluator<ServiceRequest> evaluator);

}
