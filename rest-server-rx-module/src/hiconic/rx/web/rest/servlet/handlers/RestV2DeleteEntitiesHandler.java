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
package hiconic.rx.web.rest.servlet.handlers;

import java.io.IOException;
import java.util.List;

import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.accessapi.QueryEntities;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.manipulation.CompoundManipulation;
import com.braintribe.model.generic.manipulation.DeleteManipulation;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.generic.value.PersistentEntityReference;
import com.braintribe.model.processing.query.fluent.AbstractQueryBuilder;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.query.EntityQueryResult;

import dev.hiconic.servlet.decoder.api.HttpExceptions;
import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import hiconic.rx.webapi.endpoints.v2.DdraDeleteEntitiesEndpoint;
import hiconic.rx.webapi.endpoints.v2.DdraUrlPathParameters;

public class RestV2DeleteEntitiesHandler extends AbstractEntityQueryingHandler<DdraDeleteEntitiesEndpoint> {

	@Override
	public void handle(RestV2EndpointContext<DdraDeleteEntitiesEndpoint> context) throws IOException {
		DdraUrlPathParameters parameters = context.getParameters();

		if(parameters.getEntityId() != null) {
			handleDeleteEntityById(context);
			return;
		}

		DdraDeleteEntitiesEndpoint endpoint = context.getEndpoint();
		AbstractQueryBuilder<EntityQuery> builder = decodeEntityQueryBuilder(context);

		if(parameters.getEntityId() == null && !endpoint.getAllowMultipleDelete()){
			HttpExceptions.throwPreConditionFaild("The flag 'allowMultipleDelete' must be 'true' to perform operation. This is due prevention of unintentional deletion group of entities.");
			return;
		}

		QueryEntities queryRequest = QueryEntities.T.create();
		queryRequest.setSessionId(endpoint.getSessionId());
		queryRequest.setDomainId(parameters.getAccessId());
		queryRequest.setQuery(builder.done());

		EntityQueryResult result = evaluateServiceRequest(queryRequest, true);
		List<GenericEntity> entitiesToDelete = result.getEntities();

		if(entitiesToDelete.isEmpty()) {
			writeResponse(context, project(endpoint, null, entitiesToDelete.size()), false);
			return;
		}

		ManipulationRequest manipulationRequest = getDeleteRequestFor(parameters, endpoint, entitiesToDelete);
		ManipulationResponse response = evaluateServiceRequest(manipulationRequest, true);
		writeResponse(context, project(endpoint, response, entitiesToDelete.size()), false);
	}
	
	private void handleDeleteEntityById(RestV2EndpointContext<DdraDeleteEntitiesEndpoint> context) throws IOException {
		DdraUrlPathParameters parameters = context.getParameters();
		
		PersistentEntityReference reference = getEntityReferenceFor(context);

		DdraDeleteEntitiesEndpoint endpoint = decode(context);
		
		ManipulationRequest request = createManipulationRequestFor(parameters, endpoint);
		request.setManipulation(getDeleteManipulationFor(endpoint, reference));
		ManipulationResponse response = evaluateServiceRequest(request, true);
		writeResponse(context, project(endpoint, response, 1), false);
	}
	
	private ManipulationRequest getDeleteRequestFor(DdraUrlPathParameters parameters, DdraDeleteEntitiesEndpoint endpoint, List<GenericEntity> entitiesToDelete) {
		ManipulationRequest request = createManipulationRequestFor(parameters, endpoint);

		if(entitiesToDelete.size() == 1) {
			request.setManipulation(getDeleteManipulationFor(endpoint, entitiesToDelete.get(0)));
			return request;
		}
		
		CompoundManipulation manipulation = CompoundManipulation.T.create();
		request.setManipulation(manipulation);
		for(GenericEntity entity : entitiesToDelete) {
			manipulation.getCompoundManipulationList().add(getDeleteManipulationFor(endpoint, entity));
		}
		
		return request;
	}
	
	private DeleteManipulation getDeleteManipulationFor(DdraDeleteEntitiesEndpoint endpoint, GenericEntity entity) {
		DeleteManipulation manipulation = DeleteManipulation.T.create();
		manipulation.setEntity(EntityReference.T.isInstance(entity) ? entity : entity.reference());
		manipulation.setDeleteMode(endpoint.getDeleteMode());
		return manipulation;
	}

	private Object project(DdraDeleteEntitiesEndpoint endpoint, ManipulationResponse response, int entityCount) {
		switch(endpoint.getProjection()) {
			case count:
				return entityCount;
			case envelope:
				return response;
			case success:
				return true;
			default:
				HttpExceptions.throwInternalServerError("Unexpected projection %s", endpoint.getProjection());
				return null;
			
		}
	}

	@Override
	protected DdraDeleteEntitiesEndpoint createEndpoint() {
		return DdraDeleteEntitiesEndpoint.T.create();
	}
}
