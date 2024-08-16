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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.ddra.endpoints.v2.DdraPostPropertiesEndpoint;
import com.braintribe.model.ddra.endpoints.v2.DdraUrlPathParameters;
import com.braintribe.model.generic.manipulation.AddManipulation;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.RemoveManipulation;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.ListType;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.SetType;
import com.braintribe.model.processing.web.rest.HttpExceptions;
import com.braintribe.utils.MapTools;

public class RestV2PostPropertiesHandler extends AbstractManipulationPropertiesHandler<DdraPostPropertiesEndpoint> {

	@Override
	public void handle(RestV2EndpointContext<DdraPostPropertiesEndpoint> context) throws IOException {
		DdraUrlPathParameters parameters = context.getParameters();
		checkPropertyType(context);

		DdraPostPropertiesEndpoint endpoint = decode(context);

		ManipulationRequest request = createManipulationRequestFor(parameters, endpoint);

		Object body = unmarshallBody(context, endpoint, GmDeserializationOptions.deriveDefaults().build());
		if (body == null) {
			HttpExceptions.badRequest("Unexpected body: got null.");
		}
		if (endpoint.getRemove()) {
			request.setManipulation(getRemoveManipulationFor(context, body));
		} else {
			request.setManipulation(getAddManipulationFor(context, body));
		}

		ManipulationResponse response = evaluateServiceRequest(request, true);
		writeResponse(context, project(endpoint.getProjection(), response), false);
	}

	private Manipulation getAddManipulationFor(RestV2EndpointContext<?> context, Object body) {
		AddManipulation manipulation = AddManipulation.T.create();
		computeOwnerForPropertyManipulation(manipulation, context);

		switch (context.getProperty().getType().getTypeCode()) {
			case listType:
				manipulation.setItemsToAdd(getItemsToAddForListProperty(context, body));
				break;
			case setType:
				manipulation.setItemsToAdd(getItemsToAddForSetProperty(context, body));
				break;
			case mapType:
				manipulation.setItemsToAdd(getItemsToAddForMapProperty(context, body));
				break;
			default:
				// impossible
				HttpExceptions.internalServerError("Something is wrong in the code...");
		}

		return manipulation;
	}

	private Map<Object, Object> getItemsToAddForListProperty(RestV2EndpointContext<?> context, Object body) {
		Property property = context.getProperty();
		ListType type = (ListType) property.getType();

		Map<Object, Object> result = new HashMap<>();

		if (body instanceof Collection) {
			// append the values
			Collection<?> values = (Collection<?>) body;
			int index = values.size();
			for (Object element : values) {
				// append the value
				Object value = getReferenceForPropertyValue(type.getCollectionElementType(), element, property);
				result.put(Integer.MAX_VALUE - index, value);
				index--;
			}
		} else if (body instanceof Map) {
			// keys must be integer
			// inserts the values at the given positions
			Map<Object, Object> values = (Map<Object, Object>) body;
			for (Entry<Object, Object> entry : values.entrySet()) {
				Object key = entry.getKey();
				if (!(key instanceof Integer)) {
					HttpExceptions.badRequest("keys in the body must be int, got a key od type %s", key.getClass().getName());
				}

				Object value = getReferenceForPropertyValue(type.getCollectionElementType(), entry.getValue(), property);
				result.put(key, value);
			}
		} else {
			// append the value
			Object value = getReferenceForPropertyValue(type.getCollectionElementType(), body, property);
			result.put(Integer.MAX_VALUE, value);
		}

		return result;
	}

	private Map<Object, Object> getItemsToAddForSetProperty(RestV2EndpointContext<?> context, Object body) {
		Property property = context.getProperty();
		SetType type = (SetType) property.getType();

		if (body instanceof Collection) {
			// append the values
			Map<Object, Object> result = new HashMap<>();

			Collection<?> collection = (Collection<?>) body;
			for (Object element : collection) {
				Object value = getReferenceForPropertyValue(type.getCollectionElementType(), element, property);
				result.put(value, value);
			}

			return result;
		} else {
			// append the value
			Object value = getReferenceForPropertyValue(type.getCollectionElementType(), body, property);
			return MapTools.getMap(value, value);
		}
	}

	private Map<Object, Object> getItemsToAddForMapProperty(RestV2EndpointContext<?> context, Object body) {
		Property property = context.getProperty();
		MapType type = (MapType) property.getType();
		if (body instanceof Map) {
			// put the keys/values
			Map<Object, Object> map = (Map<Object, Object>) body;
			Map<Object, Object> result = new HashMap<>();

			for (Entry<Object, Object> entry : map.entrySet()) {
				Object key = getReferenceForPropertyValue(type.getKeyType(), entry.getKey(), property);
				Object value = getReferenceForPropertyValue(type.getValueType(), entry.getValue(), property);
				result.put(key, value);
			}

			return result;
		} else {
			HttpExceptions.badRequest("Property %s is a map, expected body to be a map but got %s", property.getName(), body.getClass().getName());
			return null;
		}
	}

	private Manipulation getRemoveManipulationFor(RestV2EndpointContext<?> context, Object body) {
		RemoveManipulation manipulation = RemoveManipulation.T.create();
		computeOwnerForPropertyManipulation(manipulation, context);

		switch (context.getProperty().getType().getTypeCode()) {
			case listType:
				manipulation.setItemsToRemove(getItemsToRemoveForListProperty(context, body));
				break;
			case setType:
				manipulation.setItemsToRemove(getItemsToRemoveForSetProperty(context, body));
				break;
			case mapType:
				manipulation.setItemsToRemove(getItemsToRemoveForMapProperty(context, body));
				break;
			default:
				// impossible
				HttpExceptions.internalServerError("Something is wrong in the code...");
		}

		return manipulation;
	}

	private Map<Object, Object> getItemsToRemoveForListProperty(RestV2EndpointContext<?> context, Object body) {
		if (!(body instanceof Map)) {
			HttpExceptions.badRequest("When removing from a list property, the body must be a map<int, value> but got %s", body.getClass().getName());
		}

		Property property = context.getProperty();
		ListType type = (ListType) property.getType();

		// keys must be integer
		// removes the values at the given positions
		Map<Object, Object> result = new HashMap<>();
		Map<Object, Object> values = (Map<Object, Object>) body;
		for (Entry<Object, Object> entry : values.entrySet()) {
			Object key = entry.getKey();
			if (!(key instanceof Integer)) {
				HttpExceptions.badRequest("keys in the body must be int, got a key od type %s", key.getClass().getName());
			}

			Object value = getReferenceForPropertyValue(type.getCollectionElementType(), entry.getValue(), property);
			result.put(key, value);
		}

		return result;
	}

	private Map<Object, Object> getItemsToRemoveForSetProperty(RestV2EndpointContext<?> context, Object body) {
		// same logic as for the add, just takes the item(s) from the body.
		return getItemsToAddForSetProperty(context, body);
	}

	private Map<Object, Object> getItemsToRemoveForMapProperty(RestV2EndpointContext<?> context, Object body) {
		if (!(body instanceof Map)) {
			HttpExceptions.badRequest("When removing from a map property, the body must be a map<key, value> but got %s", body.getClass().getName());
		}

		Property property = context.getProperty();
		MapType type = (MapType) property.getType();

		// removes the values with the given keys
		Map<Object, Object> result = new HashMap<>();
		Map<Object, Object> values = (Map<Object, Object>) body;
		for (Entry<Object, Object> entry : values.entrySet()) {
			Object key = getReferenceForPropertyValue(type.getKeyType(), entry.getKey(), property);
			Object value = getReferenceForPropertyValue(type.getValueType(), entry.getValue(), property);
			result.put(key, value);
		}

		return result;
	}

	private void checkPropertyType(RestV2EndpointContext<?> context) {
		Property property = context.getProperty();
		GenericModelType type = property.getType();
		switch (type.getTypeCode()) {
			case listType:
			case setType:
			case mapType:
				// allowed
				return;
			default:
				HttpExceptions.badRequest(
						"POST for properties is only allowed for property of types list, map or set, " + "but property %s if of type: %s",
						property.getName(), type.getTypeName());
		}
	}

	@Override
	protected DdraPostPropertiesEndpoint createEndpoint() {
		return DdraPostPropertiesEndpoint.T.create();
	}
}
