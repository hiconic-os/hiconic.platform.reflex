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

import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import com.braintribe.model.accessapi.QueryProperty;
import com.braintribe.model.ddra.endpoints.v2.DdraGetPropertiesEndpoint;
import com.braintribe.model.ddra.endpoints.v2.DdraUrlPathParameters;
import com.braintribe.model.processing.query.fluent.PropertyQueryBuilder;
import com.braintribe.model.processing.web.rest.HttpExceptions;
import com.braintribe.model.query.PropertyQuery;
import com.braintribe.model.query.PropertyQueryResult;

public class RestV2GetPropertiesHandler extends AbstractQueryingHandler<DdraGetPropertiesEndpoint> {

	@Override
	public void handle(RestV2EndpointContext<DdraGetPropertiesEndpoint> context) throws IOException {
		DdraUrlPathParameters parameters = context.getParameters();
		DdraGetPropertiesEndpoint endpoint = decode(context);

		// @formatter-on
		PropertyQuery query = PropertyQueryBuilder.forProperty(context.getEntityType(), 
					parameters.getEntityId(), parameters.getEntityPartition(), parameters.getProperty())
				.tc(traversingCriteriasMap.getCriterion(endpoint.getComputedDepth()))
				.done();
		// @formatter-on
		query.setNoAbsenceInformation(endpoint.getNoAbsenceInformation());
		
		QueryProperty request = QueryProperty.T.create();
		request.setSessionId(endpoint.getSessionId());
		request.setServiceId(parameters.getAccessId());
		request.setQuery(query);
		
		PropertyQueryResult result = evaluateQueryRequest(request, endpoint, true);
		boolean full = query.getTraversingCriterion() != null;
		writeResponse(context, project(endpoint, result), full);
	}
	
	private Object project(DdraGetPropertiesEndpoint endpoint, PropertyQueryResult value) {
		switch(endpoint.getProjection()) {
			case envelope:
				return value;
			case value:
				return value.getPropertyValue();
			default:
				HttpExceptions.internalServerError("Unexpected endpoint projection %s", endpoint.getProjection());
				return null;
		}
	}

	@Override
	protected DdraGetPropertiesEndpoint createEndpoint() {
		return DdraGetPropertiesEndpoint.T.create();
	}
}
