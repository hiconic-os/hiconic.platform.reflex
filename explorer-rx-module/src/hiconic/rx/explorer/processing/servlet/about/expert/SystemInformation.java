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

import static com.braintribe.utils.lcd.CollectionTools2.isEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

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

import hiconic.rx.reflection.model.api.PlatformReflection;
import hiconic.rx.reflection.model.api.ReflectPlatform;
import hiconic.rx.reflection.model.application.RxAppInfo;
import hiconic.rx.reflection.model.check.java.JavaEnvironment;
import hiconic.rx.reflection.model.system.SystemInfo;
import hiconic.rx.servlet.velocity.TypedVelocityContext;

public class SystemInformation {

	private static Logger logger = Logger.getLogger(SystemInformation.class);

	public void processSysInfoRequest(Evaluator<ServiceRequest> requestEvaluator, Collection<InstanceId> selectedServiceInstances,
			TypedVelocityContext context, String userSessionId, ExecutorService executor) {

		Map<String, PlatformReflection> sysInfoMap = Collections.synchronizedMap(new TreeMap<>());

		AbstractMulticastingExpert.execute(selectedServiceInstances, executor, "SystemInformation", i -> {

			ReflectPlatform reflectPlatform = ReflectPlatform.T.create();

			MulticastRequest mcR = MulticastRequest.T.create();
			mcR.setAsynchronous(false);
			mcR.setServiceRequest(reflectPlatform);
			mcR.setAddressee(i);
			mcR.setTimeout((long) Numbers.MILLISECONDS_PER_MINUTE);
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
					PlatformReflection platformReflection = (PlatformReflection) envelope.getResult();

					sortCollections(platformReflection);

					sysInfoMap.put(nodeId, platformReflection);

				} else {
					logger.error("Unsupported response type: " + result);
				}

			}

		});

		context.put("sysInfoMap", sysInfoMap);

		logger.debug(() -> "Done with processing a request to return system information.");

	}

	private static void sortCollections(PlatformReflection platformReflection) {
		if (platformReflection == null)
			return;

		SystemInfo system = platformReflection.getSystemInfo();
		if (system != null) {
			JavaEnvironment environment = system.getJavaEnvironment();
			if (environment != null) {
				sortMap(environment.getSystemProperties(), environment::setSystemProperties);
				sortMap(environment.getEnvironmentVariables(), environment::setEnvironmentVariables);
			}
		}
	}

	private static void sortMap(Map<String, String> source, Consumer<Map<String, String>> consumer) {
		if (!isEmpty(source))
			consumer.accept(new TreeMap<>(source));
	}

}
