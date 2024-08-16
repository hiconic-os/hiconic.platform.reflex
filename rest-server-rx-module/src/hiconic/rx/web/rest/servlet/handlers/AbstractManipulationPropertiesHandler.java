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

import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.ddra.endpoints.v2.DdraManipulatePropertiesProjection;
import com.braintribe.model.ddra.endpoints.v2.RestV2Endpoint;
import com.braintribe.model.processing.web.rest.HttpExceptions;

public abstract class AbstractManipulationPropertiesHandler<E extends RestV2Endpoint> extends AbstractRestV2Handler<E> {

	protected Object project(DdraManipulatePropertiesProjection projection, ManipulationResponse response) {
		switch(projection) {
			case envelope:
				return response;
			case success:
				return true;
			default:
				HttpExceptions.internalServerError("Unexpected projection %s", projection);
				return null;
		}
	}

}
