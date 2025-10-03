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
import java.util.concurrent.ExecutorService;

import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.servlet.velocity.TypedVelocityContext;
import jakarta.servlet.http.HttpServletRequest;

public class HotThreadsExpert {

	private static Logger logger = Logger.getLogger(HotThreadsExpert.class);

	public void processHotThreadsRequest(Evaluator<ServiceRequest> requestEvaluator, Collection<InstanceId> selectedServiceInstances,
			HttpServletRequest request, TypedVelocityContext context, String userSessionId, ExecutorService executor) {

		throw new UnsupportedOperationException("Method 'HotThreadsExpert.processHotThreadsRequest' is not implemented yet!");

		// TODO implement hot threads
		
//		final Map<String, String> hotThreadsMap = Collections.synchronizedMap(new TreeMap<>());
//
//		AbstractMulticastingExpert.execute(selectedServiceInstances, executor, "HotThreads", i -> {
//			GetHotThreads getHotThreads = GetHotThreads.T.create();
//			getHotThreads.setInterval(getSingleParameterAsLong(request, "interval"));
//			getHotThreads.setThreads(getSingleParameterAsInteger(request, "threads"));
//			getHotThreads.setIgnoreIdleThreads(getSingleParameterAsBoolean(request, "ignoreIdleThreads"));
//			getHotThreads.setSampleType(getSingleParameterAsString(request, "sampleType"));
//			getHotThreads.setThreadElementsSnapshotCount(getSingleParameterAsInteger(request, "threadElementsSnapshotCount"));
//			getHotThreads.setThreadElementsSnapshotDelayInMs(getSingleParameterAsLong(request, "threadElementsSnapshotDelayInMs"));
//
//			MulticastRequest mcR = MulticastRequest.T.create();
//			mcR.setAsynchronous(false);
//			mcR.setServiceRequest(getHotThreads);
//			mcR.setAddressee(i);
//			mcR.setTimeout((long) Numbers.MILLISECONDS_PER_MINUTE);
//			mcR.setSessionId(userSessionId);
//			EvalContext<? extends MulticastResponse> eval = mcR.eval(requestEvaluator);
//			MulticastResponse multicastResponse = eval.get();
//
//			for (Map.Entry<InstanceId, ServiceResult> entry : multicastResponse.getResponses().entrySet()) {
//
//				InstanceId instanceId = entry.getKey();
//
//				logger.debug(() -> "Received a response from instance: " + instanceId);
//
//				String nodeId = instanceId.getNodeId();
//
//				ServiceResult result = entry.getValue();
//				if (result instanceof Failure) {
//					Throwable throwable = FailureCodec.INSTANCE.decode(result.asFailure());
//					logger.error("Received failure from " + instanceId, throwable);
//				} else if (result instanceof ResponseEnvelope) {
//
//					ResponseEnvelope envelope = (ResponseEnvelope) result;
//					HotThreads hotThreads = (HotThreads) envelope.getResult();
//
//					String stringRepresentation = PlatformReflectionTools.toString(hotThreads);
//
//					hotThreadsMap.put(nodeId, stringRepresentation);
//
//				} else {
//					logger.error("Unsupported response type: " + result);
//				}
//
//			}
//		});
//
//		context.put("hotthreadsMap", hotThreadsMap);
//
//		logger.debug(() -> "Done with processing a request to return hot threads.");
	}
}
