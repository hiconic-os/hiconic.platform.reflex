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

import java.util.List;
import java.util.Map;

import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.SimpleTypes;
import com.braintribe.model.openapi.v3_0.OpenApi;
import com.braintribe.model.openapi.v3_0.OpenapiOperation;
import com.braintribe.model.openapi.v3_0.OpenapiParameter;
import com.braintribe.model.openapi.v3_0.OpenapiPath;
import com.braintribe.model.openapi.v3_0.OpenapiRequestBody;
import com.braintribe.model.openapi.v3_0.api.OpenapiPropertiesRequest;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.webapi.endpoints.v2.DdraDeletePropertiesEndpoint;
import hiconic.rx.webapi.endpoints.v2.DdraGetPropertiesEndpoint;
import hiconic.rx.webapi.endpoints.v2.DdraPutPropertiesEndpoint;

/**
 * This processor generically creates an {@link OpenApi} document for REST property queries for a given access.
 * 
 * @author Neidhart.Orlich
 *
 */
public class PropertyOpenapiProcessor extends AbstractCrudOpenapiProcessor<OpenapiPropertiesRequest> {

	private static final EntityType<?> manipulationResponseType = ManipulationResponse.T;

	@Override
	protected void init() {
		// create responses in the standardComponentsContext
		registerStandardResponse("200", manipulationResponseType, "Successful manipulation result.");
	}

	@Override
	protected void process(OpenapiContext sessionScopedContext, OpenapiPropertiesRequest request, OpenApi openApi) {
		Map<String, OpenapiPath> paths = openApi.getPaths();
		
		ComponentScope sessionEndpointScope = new ComponentScope(sessionScopedContext.getComponentScope(), standardComponentsContext.getComponentScope().getCmdResolver());
		OpenapiContext endpointParametersResolvingContext = standardComponentsContext.childContext("-ENDPOINTS-SESSION", sessionEndpointScope, OpenapiMimeType.URLENCODED); 
		endpointParametersResolvingContext.transferRequestDataFrom(sessionScopedContext);
		
		List<OpenapiParameter> endpointParametersGet = getQueryParameterRefs(DdraGetPropertiesEndpoint.T, endpointParametersResolvingContext, "endpoint");
		List<OpenapiParameter> endpointParametersDelete = getQueryParameterRefs(DdraDeletePropertiesEndpoint.T, endpointParametersResolvingContext, "endpoint");
		List<OpenapiParameter> endpointParametersPut = getQueryParameterRefs(DdraPutPropertiesEndpoint.T, endpointParametersResolvingContext, "endpoint");

		modelEntitiesSorted(sessionScopedContext).forEach(e -> {
			e.getProperties().stream()
				.filter(p -> p.getDeclaringType() != GenericEntity.T)
				.forEach(p -> {
					String key = getPathKey("", e, "", true) + "/{id}/" + p.getName();
					paths.put(key, createPath(sessionScopedContext, p, e, endpointParametersGet, endpointParametersDelete, endpointParametersPut));
				});
		});
	}

	private OpenapiPath createPath(OpenapiContext context, Property property, EntityType<?> entityType, List<OpenapiParameter> endpointParametersGet, List<OpenapiParameter> endpointParametersDelete, List<OpenapiParameter> endpointParametersPut) {

		OpenapiOperation get = createOperation(context, property.getType());
		OpenapiOperation delete = createOperation(context, SimpleTypes.TYPE_BOOLEAN);
		OpenapiOperation put = createOperation(context, manipulationResponseType);

		get.getParameters().add(getIdParamRef(context));
		delete.getParameters().add(getIdParamRef(context));
		put.getParameters().add(getIdParamRef(context));

		get.getParameters().addAll(endpointParametersGet);
		delete.getParameters().addAll(endpointParametersDelete);
		put.getParameters().addAll(endpointParametersPut);
		
		GenericModelType propertyType = property.getType();
		OpenapiRequestBody reqestBody = context.components().requestBody(propertyType).ensure(currentContext -> {
			OpenapiRequestBody b = OpenapiRequestBody.T.create();
			b.setContent(createContent(propertyType, currentContext));
			b.setRequired(true);
			b.setDescription("Serialized " + propertyType.getTypeSignature());
			return b;
		}).getRef();

		put.setRequestBody(reqestBody);

		String typeSignature = entityType.getTypeSignature();
		int lastDotIndex = typeSignature.lastIndexOf('.');
		String simpleName = typeSignature.substring(lastDotIndex + 1);
		String packageName = typeSignature.substring(0, lastDotIndex);
		String tag = simpleName + " (" + packageName + ")";

		get.getTags().add(tag);
		delete.getTags().add(tag);
		put.getTags().add(tag);
		
		get.setDescription("Get the value of the *" + property.getName() + "* property of the *" + tag + "* with a certain id.");
		delete.setDescription("Delete the value of the *" + property.getName() + "* property of the *" + tag + "* with a certain id.");
		put.setDescription("Update the value of the *" + property.getName() + "* property of the *" + tag + "* with a certain id.");

		OpenapiPath path = OpenapiPath.T.create();
		path.setGet(get);
		path.setDelete(delete);
		path.setPut(put);

		return path;
	}

	private OpenapiParameter getIdParamRef(OpenapiContext context) {
		OpenapiParameter idParameter = context.components().parameter("PATH_ID").ensure(currentContext -> {
			OpenapiParameter param = OpenapiParameter.T.create();
			param.setName("id");
			param.setIn("path");
			param.setRequired(true);
			param.setSchema(getSchema(SimpleTypes.TYPE_STRING, currentContext));
			param.setDescription("The id of the entity to be adressed.");
			return param;
		}).getRef();
		return idParameter;
	}

	private OpenapiOperation createOperation(OpenapiContext context, GenericModelType responseType) {
		OpenapiOperation operation = OpenapiOperation.T.create();
		addResponsesToOperation(responseType, operation, context, true);
		return operation;
	}

	@Override
	protected String getTitle(ServiceRequestContext requestContext, OpenapiPropertiesRequest request) {
		return "CRUD properties in " + request.getAccessId();
	}

	@Override
	protected String getRelativeEndpointPath(ServiceRequestContext requestContext, OpenapiPropertiesRequest request) {
		return "rest/properties/" + request.getAccessId();
	}

}
