// ============================================================================
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

import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.service.api.result.Unsatisfied;

import hiconic.rx.web.rest.servlet.RestV2EndpointContext;
import hiconic.rx.webapi.endpoints.v2.DdraErrorEndpoint;

public class PathErrorHandler extends AbstractRestV2Handler<DdraErrorEndpoint> {
	private Maybe<?> maybe;
	
	public PathErrorHandler(MarshallerRegistry marshallerRegistry, Maybe<?> maybe) {
		super();
		this.maybe = maybe;
		this.marshallerRegistry = marshallerRegistry;
	}

	@Override
	protected DdraErrorEndpoint createEndpoint() {
		return DdraErrorEndpoint.T.create();
	}
	
	@Override
	public void handle(RestV2EndpointContext<DdraErrorEndpoint> context) throws IOException {
		
		decode(context);
		Unsatisfied unsatisfied = Unsatisfied.from(maybe);
		context.setForceResponseCode(400);
		writeResponse(context, unsatisfied, true);
	}
}
