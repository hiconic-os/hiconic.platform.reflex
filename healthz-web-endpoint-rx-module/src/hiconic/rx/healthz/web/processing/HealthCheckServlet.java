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
package hiconic.rx.healthz.web.processing;

import java.io.IOException;
import java.util.Map;

import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.api.MarshallerRegistry;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.api.aspect.HttpStatusCodeNotification;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Unsatisfied;

import hiconic.rx.check.model.api.request.RunHealthChecks;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Exposes the CX-compatible, aggregated health-check response at {@code <default-endpoints-base-path>/healthz}. */
public class HealthCheckServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String JSON = "application/json";

	private final Evaluator<ServiceRequest> evaluator;
	private final Marshaller jsonMarshaller;

	public HealthCheckServlet(Evaluator<ServiceRequest> evaluator, MarshallerRegistry marshallerRegistry) {
		this.evaluator = evaluator;
		this.jsonMarshaller = marshallerRegistry.getMarshaller(JSON);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		int[] responseStatus = { HttpServletResponse.SC_OK };

		RunHealthChecks healthChecks = RunHealthChecks.T.create();
		EvalContext<Map<InstanceId, Object>> evalContext = healthChecks.eval(evaluator) //
				.with(HttpStatusCodeNotification.class, status -> responseStatus[0] = status);
		Maybe<Map<InstanceId, Object>> result = evalContext.getReasoned();

		Object responseBody;
		if (result.isSatisfied()) {
			responseBody = result.get();
		} else {
			if (responseStatus[0] == HttpServletResponse.SC_OK)
				responseStatus[0] = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
			responseBody = Unsatisfied.from(result);
		}

		response.setStatus(responseStatus[0]);
		response.setContentType(JSON);
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-store");
		jsonMarshaller.marshall(response.getOutputStream(), responseBody);
	}
}
