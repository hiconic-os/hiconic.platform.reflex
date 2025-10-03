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

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import com.braintribe.logging.Logger;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.ServiceRequest;

import jakarta.servlet.http.HttpServletResponse;

public class Json {

	private static Logger logger = Logger.getLogger(Json.class);

	public void processJsonRequest(Evaluator<ServiceRequest> requestEvaluator, Collection<InstanceId> selectedServiceInstances,
			HttpServletResponse resp, String userSessionId, ExecutorService executor) throws IOException {

		logger.debug(() -> "Processing a request to create a JSON response.");
		resp.getWriter().append("TODO implement Json.processJsonRequest");

//		ReentrantLock lock = new ReentrantLock();
//		StringBuilder sb = new StringBuilder();
//		sb.append("[\n");
//
//		AbstractMulticastingExpert.execute(selectedServiceInstances, executor, "Json", i -> {
//
//			// First, we prepare the actual content
//			ReflectPlatformJson reflectPlatform = ReflectPlatformJson.T.create();
//
//			MulticastRequest mcR = MulticastRequest.T.create();
//			mcR.setAsynchronous(false);
//			mcR.setServiceRequest(reflectPlatform);
//			mcR.setAddressee(i);
//			mcR.setTimeout((long) Numbers.MILLISECONDS_PER_MINUTE);
//			mcR.setSessionId(userSessionId);
//			EvalContext<? extends MulticastResponse> eval = mcR.eval(requestEvaluator);
//			MulticastResponse multicastResponse = eval.get();
//
//			for (Map.Entry<InstanceId, ServiceResult> entry : multicastResponse.getResponses().entrySet()) {
//				InstanceId instance = entry.getKey();
//
//				logger.debug(() -> "Received a response from instance: " + instance);
//
//				ServiceResult result = entry.getValue();
//				if (result instanceof Failure) {
//					Throwable throwable = FailureCodec.INSTANCE.decode(result.asFailure());
//					logger.error("Received failure from " + instance, throwable);
//				} else if (result instanceof ResponseEnvelope) {
//
//					ResponseEnvelope envelope = (ResponseEnvelope) result;
//					PlatformReflectionJson platformReflection = (PlatformReflectionJson) envelope.getResult();
//					lock.lock();
//					try {
//						if (sb.toString().endsWith("}")) {
//							sb.append(",\n");
//						}
//						sb.append("\n{");
//						sb.append("  \"" + instance + "\" : \n");
//						sb.append(platformReflection.getPlatformReflectionJson());
//						sb.append("\n}");
//					} finally {
//						lock.unlock();
//					}
//
//				} else {
//					logger.error("Unsupported response type: " + result);
//				}
//
//			}
//
//		});
//
//		sb.append("\n]");
//
//		String now = DateTools.encode(new Date(), DateTools.TERSE_DATETIME_FORMAT_2);
//
//		// Only when we were successful up to this point, we actually write the content to the client
//		// This allows the ExceptionFilter to be able to write to the response stream without problems
//		resp.setContentType("application/json; charset=UTF-8");
//		resp.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", "sysinfo-" + now + ".json"));
//		resp.getWriter().append(sb);
//
//		logger.debug(() -> "Done with processing a request to create a JSON response.");
	}

}
