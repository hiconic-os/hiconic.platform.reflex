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
package hiconic.rx.explorer.processing.servlet.about.expert;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;

import com.braintribe.common.lcd.Numbers;
import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.common.FailureCodec;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Failure;
import com.braintribe.model.service.api.result.MulticastResponse;
import com.braintribe.model.service.api.result.ResponseEnvelope;
import com.braintribe.model.service.api.result.ServiceResult;

import hiconic.rx.reflection.model.api.GetProcesses;
import hiconic.rx.reflection.model.api.Processes;
import hiconic.rx.servlet.velocity.TypedVelocityContext;

public class ProcessesExpert {

	private static Logger logger = Logger.getLogger(ProcessesExpert.class);

	public void processProcessesRequest(Evaluator<ServiceRequest> requestEvaluator, Collection<InstanceId> selectedServiceInstances,
			TypedVelocityContext context, String userSessionId, ExecutorService executor) {

		Map<String, Processes> processesMap = Collections.synchronizedMap(new TreeMap<>());

		AbstractMulticastingExpert.execute(selectedServiceInstances, executor, "Processes", i -> {

			GetProcesses getProcesses = GetProcesses.T.create();

			MulticastRequest mcR = MulticastRequest.T.create();
			mcR.setAsynchronous(false);
			mcR.setServiceRequest(getProcesses);
			mcR.setAddressee(i);
			mcR.setTimeout((long) Numbers.MILLISECONDS_PER_MINUTE * 2);
			mcR.setSessionId(userSessionId);
			EvalContext<? extends MulticastResponse> eval = mcR.eval(requestEvaluator);
			MulticastResponse multicastResponse = eval.get();

			for (Map.Entry<InstanceId, ServiceResult> entry : multicastResponse.getResponses().entrySet()) {

				InstanceId instanceId = entry.getKey();

				logger.debug(() -> "Received a response from instance: " + instanceId);

				String nodeId = instanceId.getNodeId();

				ServiceResult result = entry.getValue();
				if (result instanceof Failure) {
					Throwable throwable = FailureCodec.INSTANCE.decode(result.asFailure());
					logger.error("Received failure from " + instanceId, throwable);
				} else if (result instanceof ResponseEnvelope) {

					ResponseEnvelope envelope = (ResponseEnvelope) result;
					Processes processes = (Processes) envelope.getResult();

					processesMap.put(nodeId, processes);

				} else {
					logger.error("Unsupported response type: " + result);
				}

			}

		});

		context.put("processesMap", processesMap);

		logger.debug(() -> "Done with processing a request to return information about processes.");
	}

}
