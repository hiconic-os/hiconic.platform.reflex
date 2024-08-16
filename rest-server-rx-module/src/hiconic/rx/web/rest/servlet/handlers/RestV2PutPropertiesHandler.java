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

import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.ddra.endpoints.v2.DdraPutPropertiesEndpoint;
import com.braintribe.model.ddra.endpoints.v2.DdraUrlPathParameters;
import com.braintribe.model.generic.manipulation.ChangeValueManipulation;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.processing.web.rest.HttpExceptions;

public class RestV2PutPropertiesHandler extends AbstractManipulationPropertiesHandler<DdraPutPropertiesEndpoint> {

	@Override
	public void handle(RestV2EndpointContext<DdraPutPropertiesEndpoint> context) throws IOException {
		handlePutAndPatch(context);
	}

	public void handlePutAndPatch(RestV2EndpointContext<DdraPutPropertiesEndpoint> context) throws IOException {
		DdraUrlPathParameters parameters = context.getParameters();
		checkPropertyType(context);

		DdraPutPropertiesEndpoint endpoint = decode(context);

		ManipulationRequest request = createManipulationRequestFor(parameters, endpoint);

		ChangeValueManipulation manipulation = ChangeValueManipulation.T.create();
		request.setManipulation(manipulation);

		computeOwnerForPropertyManipulation(manipulation, context);

		GmDeserializationOptions options = GmDeserializationOptions.deriveDefaults().setInferredRootType(context.getProperty().getType()).build();

		Object newValue = getReferenceForPropertyValue(context.getProperty().getType(), unmarshallBody(context, endpoint, options),
				context.getProperty());
		manipulation.setNewValue(newValue);

		ManipulationResponse response = evaluateServiceRequest(request, true);
		writeResponse(context, project(endpoint.getProjection(), response), false);
	}

	private void checkPropertyType(RestV2EndpointContext<?> context) {
		Property property = context.getProperty();
		GenericModelType type = property.getType();
		switch (type.getTypeCode()) {
			case listType:
			case setType:
			case mapType:
				HttpExceptions.badRequest(context.getRequest().getMethod() + " for properties is only allowed for non-collection properties, "
						+ "but property %s if of type: %s", property.getName(), type.getTypeName());
				return;
			default:
				// allowed.
		}
	}

	@Override
	protected DdraPutPropertiesEndpoint createEndpoint() {
		return DdraPutPropertiesEndpoint.T.create();
	}
}
