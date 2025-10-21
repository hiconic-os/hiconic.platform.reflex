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
package hiconic.rx.openapi.v3.processing.processor.export;

import java.util.HashMap;
import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.openapi.v3_0.OpenApi;
import com.braintribe.model.openapi.v3_0.OpenapiComponents;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;

import hiconic.rx.webapi.endpoints.DdraEndpoint;

/**
 * This class describes a resolving scope for {@link OpenapiComponents}. A scope has a certain {@link GmMetaModel} which
 * is used as a base to resolve the {@link OpenapiComponents}. These components are reusable and can be shared between
 * {@link OpenapiContext}s and even {@link OpenApi} documents. An {@link OpenApi} document is created with components
 * from multiple scopes because
 * <li>A general scope is used to cache common components
 * <li>{@link OpenApi} documents are resolved from {@link GenericEntity}s from different models. I.e. the generic query
 * {@link DdraEndpoint} parameters are modeled independently from the requests of a specific service model.
 *
 * @author Neidhart.Orlich
 */
public class ComponentScope {
	public static final String USECASE_DDRA = "ddra";
	public static final String USECASE_OPENAPI = "openapi";
	public static final String USECASE_INCLUDE_SESSION_ID = USECASE_OPENAPI + ":include-session-id";

	private Map<String, String> shortToFullRefKey = new HashMap<>();
	private final GmMetaModel model;
	private final ModelOracle modelOracle;
	private final CmdResolver cmdResolver;
	private final OpenapiComponents components;

	public ComponentScope(ComponentScope components, CmdResolver cmdResolver) {
		this(components.components, cmdResolver);
		this.shortToFullRefKey = components.shortToFullRefKey;
	}
	
	public ComponentScope(OpenapiComponents components, CmdResolver cmdResolver) {
		this.cmdResolver = cmdResolver;
		this.modelOracle = cmdResolver.getModelOracle();
		this.model = modelOracle.getGmMetaModel();

		this.components = components;
	}
	
	public void transferFrom(ComponentScope otherScope) {
		OpenapiComponents otherComponents = otherScope.components;
		
		components.getSchemas().putAll(otherComponents.getSchemas());
		components.getHeaders().putAll(otherComponents.getHeaders());
		components.getResponses().putAll(otherComponents.getResponses());
		components.getParameters().putAll(otherComponents.getParameters());
		
		if (shortToFullRefKey.isEmpty()) {
			shortToFullRefKey.putAll(otherScope.shortToFullRefKey);
		} else {
			otherScope.shortToFullRefKey.entrySet().forEach(e -> 
				shortToFullRefKey.merge(e.getKey(), e.getValue(), (k,v) -> {return handleReAssignment(k, v, e.getValue());})
			);
		}
	}

	private String handleReAssignment(String k, String v, String newValue) {
		if (v.equals(newValue))
			return v;
		
		throw new IllegalStateException("Short refKey '" + k + "' was already assigned to full key '" + v + "' but re-assignment to '" + newValue + "' was attempted");
	}

	public OpenapiComponents components() {
		return components;
	}

	public GmMetaModel getModel() {
		return model;
	}

	public ModelOracle getModelOracle() {
		return modelOracle;
	}

	public CmdResolver getCmdResolver() {
		return cmdResolver;
	}

	public String getFullRefKey(String shortRefKey) {
		return shortToFullRefKey.get(shortRefKey);
	}
	
	public void registerShortRefKey(String shortRefKey, String fullRefKey) {
		shortToFullRefKey.put(shortRefKey, fullRefKey);
	}
}
