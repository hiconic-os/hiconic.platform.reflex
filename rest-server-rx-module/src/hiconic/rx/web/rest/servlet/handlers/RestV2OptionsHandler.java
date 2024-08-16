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
import java.util.Arrays;

import com.braintribe.model.ddra.endpoints.v2.RestV2Endpoint;

import dev.hiconic.servlet.ddra.endpoints.api.DdraEndpointsUtils;
import hiconic.rx.web.rest.servlet.RestV2EndpointContext;

public class RestV2OptionsHandler extends AbstractRestV2Handler<RestV2Endpoint> {

	@Override
	public void handle(RestV2EndpointContext<RestV2Endpoint> context) throws IOException {
		context.getResponse().setStatus(204);
		DdraEndpointsUtils.setAllowHeader(context, Arrays.asList("DELETE", "GET", "POST", "PUT", "PATCH"));
	}

	@Override
	protected RestV2Endpoint createEndpoint() {
		return null; // no specific endpoint options
	}


}
