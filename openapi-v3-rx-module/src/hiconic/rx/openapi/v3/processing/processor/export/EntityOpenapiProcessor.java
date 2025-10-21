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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.SimpleTypes;
import com.braintribe.model.openapi.v3_0.OpenApi;
import com.braintribe.model.openapi.v3_0.OpenapiOperation;
import com.braintribe.model.openapi.v3_0.OpenapiParameter;
import com.braintribe.model.openapi.v3_0.OpenapiPath;
import com.braintribe.model.openapi.v3_0.OpenapiRequestBody;
import com.braintribe.model.openapi.v3_0.api.OpenapiEntitiesRequest;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.query.EntityQueryResult;

import hiconic.rx.webapi.endpoints.v2.DdraDeleteEntitiesEndpoint;
import hiconic.rx.webapi.endpoints.v2.DdraDeleteEntitiesEndpointBase;
import hiconic.rx.webapi.endpoints.v2.DdraGetEntitiesEndpoint;
import hiconic.rx.webapi.endpoints.v2.DdraGetEntitiesEndpointBase;
import hiconic.rx.webapi.endpoints.v2.DdraManipulateEntitiesEndpoint;

/**
 * This processor generically creates an {@link OpenApi} document for REST entity queries for a given access. 
 * 
 * @author Neidhart.Orlich
 *
 */
public class EntityOpenapiProcessor extends AbstractCrudOpenapiProcessor<OpenapiEntitiesRequest> {

	private EntityType<?> entityQueryResultType;
	private EntityType<?> manipulationResponseType;

	@Override
	protected void init() {
		entityQueryResultType = EntityQueryResult.T;
		manipulationResponseType = ManipulationResponse.T;

		// create responses in the standardComponentsContext
		registerStandardResponse("200", entityQueryResultType, "Successful query result.");
		registerStandardResponse("200", manipulationResponseType, "Successful manipulation result.");
	}

	@Override
	protected void process(OpenapiContext sessionScopedContext, OpenapiEntitiesRequest request, OpenApi openApi) {
		Map<String, OpenapiPath> paths = openApi.getPaths();
		
		ComponentScope sessionEndpointScope = new ComponentScope(sessionScopedContext.getComponentScope(), standardComponentsContext.getComponentScope().getCmdResolver());
		OpenapiContext endpointParametersResolvingContext = standardComponentsContext.childContext("-ENDPOINTS-SESSION", sessionEndpointScope, OpenapiMimeType.URLENCODED); 
		endpointParametersResolvingContext.transferRequestDataFrom(sessionScopedContext);
		
		List<OpenapiParameter> endpointParametersGet = getQueryParameterRefs(DdraGetEntitiesEndpoint.T, endpointParametersResolvingContext, "endpoint");
		List<OpenapiParameter> endpointParametersGetWithId = getQueryParameterRefs(DdraGetEntitiesEndpointBase.T, endpointParametersResolvingContext, "endpoint");
		List<OpenapiParameter> endpointParametersDelete = getQueryParameterRefs(DdraDeleteEntitiesEndpoint.T, endpointParametersResolvingContext, "endpoint");
		List<OpenapiParameter> endpointParametersDeleteWithId = getQueryParameterRefs(DdraDeleteEntitiesEndpointBase.T, endpointParametersResolvingContext, "endpoint");
		List<OpenapiParameter> endpointParametersManipulate = getQueryParameterRefs(DdraManipulateEntitiesEndpoint.T, endpointParametersResolvingContext, "endpoint");

		modelEntitiesSorted(sessionScopedContext).forEach(t -> {
			String key = getPathKey("", t, "", true);
			paths.put(key, createPath(sessionScopedContext, t, false, false, endpointParametersGet, endpointParametersDelete, endpointParametersManipulate));
			paths.put(key + "/{id}", createPath(sessionScopedContext, t, true, false, endpointParametersGetWithId, endpointParametersDeleteWithId, endpointParametersManipulate));
			
			if (request.getEnablePartition())
				paths.put(key + "/{id}/{partition}", createPath(sessionScopedContext, t, true, true, endpointParametersGetWithId, endpointParametersDeleteWithId, endpointParametersManipulate));
		});

	}
	
	private String operationDescription(EntityType<?> type, String endpointAction) {
		return "This endpoint allows you to " + endpointAction + " *" + type.getTypeSignature() + "* resources.";
	}

	private OpenapiPath createPath(OpenapiContext context, EntityType<?> t, boolean withId, boolean withPartition, List<OpenapiParameter> endpointParametersGet, List<OpenapiParameter> endpointParametersDelete, List<OpenapiParameter> endpointParametersManipulate) {
		OpenapiOperation get = createOperation(context, entityQueryResultType, operationDescription(t, "retrieve"));
		OpenapiOperation delete = createOperation(context, SimpleTypes.TYPE_INTEGER, operationDescription(t, "delete")); // returns number of deleted entities
		OpenapiOperation patch = createOperation(context, manipulationResponseType, operationDescription(t, "patch"));
		OpenapiOperation post = createOperation(context, manipulationResponseType, operationDescription(t, "create"));
		OpenapiOperation put = createOperation(context, manipulationResponseType, operationDescription(t, "update"));

		if (withId) {
			get.getParameters().add(getIdParamRef(context));
			delete.getParameters().add(getIdParamRef(context));
			patch.getParameters().add(getIdParamRef(context));
			post.getParameters().add(getIdParamRef(context));
			put.getParameters().add(getIdParamRef(context));
			
			if (withPartition) {
				get.getParameters().add(getPartitionParamRef(context));
				delete.getParameters().add(getPartitionParamRef(context));
				patch.getParameters().add(getPartitionParamRef(context));
				post.getParameters().add(getPartitionParamRef(context));
				put.getParameters().add(getPartitionParamRef(context));
			}
		} else {
			List<OpenapiParameter> endpointParametersWhere = getWhereParameterRefs(t, context);
			get.getParameters().addAll(endpointParametersWhere);
			delete.getParameters().addAll(endpointParametersWhere);
		}
		
		get.getParameters().addAll(endpointParametersGet);
		delete.getParameters().addAll(endpointParametersDelete);

		if (!t.isAbstract()) {
			patch.getParameters().addAll(endpointParametersManipulate);
			post.getParameters().addAll(endpointParametersManipulate);
			put.getParameters().addAll(endpointParametersManipulate);
		}

		OpenapiRequestBody requestBody = context.components().requestBody(t).ensure(currentContext -> {
			OpenapiRequestBody b = OpenapiRequestBody.T.create();
			b.setContent(createContent(t, currentContext));
			b.setRequired(true);
			b.setDescription("Serialized " + t.getTypeSignature());
			return b;
		}).getRef();

		patch.setRequestBody(requestBody);
		post.setRequestBody(requestBody);
		put.setRequestBody(requestBody);

		String typeSignature = t.getTypeSignature();
		int lastDotIndex = typeSignature.lastIndexOf('.');
		String simpleName = typeSignature.substring(lastDotIndex + 1);
		String packageName = typeSignature.substring(0, lastDotIndex);
		String tag = simpleName + " (" + packageName + ")";

		get.getTags().add(tag);
		delete.getTags().add(tag);
		patch.getTags().add(tag);
		post.getTags().add(tag);
		put.getTags().add(tag);

		OpenapiPath path = OpenapiPath.T.create();
		path.setGet(get);
		path.setDelete(delete);
		path.setPatch(patch);
		path.setPost(post);
		path.setPut(put);

		return path;
	}

	public List<OpenapiParameter> getWhereParameterRefs(EntityType<?> entityType, OpenapiContext context) {
		List<OpenapiParameter> parameters = new ArrayList<>();
		
		for (Property p : entityType.getProperties()) {
			if (p.isIdentifying() || p.isGlobalId() || !p.getType().isScalar()) {
				continue;
			}
			
			OpenapiParameter parameterReference = context.components().parameter(entityType, p, "where") //
				.ensure(currentContext -> createWhereParameter(p, currentContext)) //
				.getRef();
			
			parameters.add(parameterReference);
		}
		
		return Collections.unmodifiableList(parameters);
	}

	private OpenapiParameter createWhereParameter(Property property, OpenapiContext context) {
		OpenapiParameter parameter = OpenapiParameter.T.create();
		String name = "where." + property.getName();

		String description = "Select entities where the **" + property.getName() + "** property has a certain value.";
		parameter.setDescription(description);

		parameter.setName(name);
		parameter.setSchema(getSchema(property.getType(), context));
		parameter.setIn("query");

		return parameter;
	}

	private OpenapiParameter getIdParamRef(OpenapiContext context) {
		OpenapiParameter idParameter = context.components().parameter("PATH_ID").ensure(currentContext -> {
			OpenapiParameter param = OpenapiParameter.T.create();
			param.setName("id");
			param.setIn("path");
			param.setRequired(true);
			param.setSchema(getSchema(SimpleTypes.TYPE_STRING, currentContext));
			return param;
		}).getRef();
		return idParameter;
	}
	
	private OpenapiParameter getPartitionParamRef(OpenapiContext context) {
		OpenapiParameter idParameter = context.components().parameter("PATH_PARTITION").ensure(currentContext -> {
			OpenapiParameter param = OpenapiParameter.T.create();
			param.setName("partition");
			param.setIn("path");
			param.setRequired(true);
			param.setSchema(getSchema(SimpleTypes.TYPE_STRING, currentContext));
			return param;
		}).getRef();
		return idParameter;
	}

	private OpenapiOperation createOperation(OpenapiContext context, GenericModelType responseType, String description) {
		OpenapiOperation operation = OpenapiOperation.T.create();
		addResponsesToOperation(responseType, operation, context, true);
		operation.setDescription(description);
		return operation;
	}

	@Override
	protected String getTitle(ServiceRequestContext requestContext, OpenapiEntitiesRequest request) {
		return "CRUD entities in " + request.getAccessId();
	}

	@Override
	protected String getRelativeEndpointPath(ServiceRequestContext requestContext, OpenapiEntitiesRequest request) {
		return "rest/entities/" + request.getAccessId();
	}
	
}
