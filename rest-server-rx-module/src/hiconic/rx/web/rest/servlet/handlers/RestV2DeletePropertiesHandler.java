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
import java.math.BigDecimal;

import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import com.braintribe.exception.HttpException;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.ddra.endpoints.v2.DdraDeletePropertiesEndpoint;
import com.braintribe.model.ddra.endpoints.v2.DdraUrlPathParameters;
import com.braintribe.model.generic.manipulation.ChangeValueManipulation;
import com.braintribe.model.generic.manipulation.ClearCollectionManipulation;
import com.braintribe.model.generic.manipulation.PropertyManipulation;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.processing.web.rest.HttpExceptions;

public class RestV2DeletePropertiesHandler extends AbstractRestV2Handler<DdraDeletePropertiesEndpoint> {

	@Override
	public void handle(RestV2EndpointContext<DdraDeletePropertiesEndpoint> context) throws IOException {
		DdraUrlPathParameters parameters = context.getParameters();
		DdraDeletePropertiesEndpoint endpoint = decode(context);

		ManipulationRequest request = createManipulationRequestFor(parameters, endpoint);
		computeManipulation(context, request);

		ManipulationResponse response = evaluateServiceRequest(request, true);
		writeResponse(context, project(endpoint, response), false);
	}
	
	private Object project(DdraDeletePropertiesEndpoint endpoint, ManipulationResponse response) {
		switch(endpoint.getProjection()) {
			case envelope:
				return response;
			case success:
				return true;
			default:
				HttpExceptions.internalServerError("Unexpected projection %s", endpoint.getProjection());
				return null;
		}
	}

	private void computeManipulation(RestV2EndpointContext<?> context, ManipulationRequest request) {
		PropertyManipulation manipulation = getManipulation(context);
		computeOwnerForPropertyManipulation(manipulation, context);
		request.setManipulation(manipulation);
	}
	
	private PropertyManipulation getManipulation(RestV2EndpointContext<?> context) {
		GenericModelType type = context.getProperty().getType();
		if(type.isCollection()) {
			return ClearCollectionManipulation.T.create();
		} else {
			ChangeValueManipulation manipulation = ChangeValueManipulation.T.create();
			manipulation.setNewValue(getNullValueFor(type));
			return manipulation;
		}
	}
	
	private Object getNullValueFor(GenericModelType type) {
		switch(type.getTypeCode()) {
			case booleanType:
				return false;
			case decimalType:
				return BigDecimal.ZERO;
			case doubleType:
				return 0.0;
			case floatType:
				return 0f;
			case integerType:
				return 0;
			case longType:
				return 0L;
			case dateType:
			case entityType:
			case enumType:
			case objectType:
			case stringType:
				return null;
			case listType:
			case setType:
			case mapType:
			default:
				throw new HttpException(500, "Unexpected type: " + type.getTypeName());
		}
	}

	@Override
	protected DdraDeletePropertiesEndpoint createEndpoint() {
		return DdraDeletePropertiesEndpoint.T.create();
	}
}
