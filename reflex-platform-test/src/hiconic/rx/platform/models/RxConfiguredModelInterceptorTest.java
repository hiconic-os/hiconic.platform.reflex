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
package hiconic.rx.platform.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.braintribe.model.processing.service.api.InterceptorKind;
import com.braintribe.model.processing.service.api.ServiceAroundProcessor;
import com.braintribe.model.processing.service.api.ServicePostProcessor;
import com.braintribe.model.processing.service.api.ServicePreProcessor;
import com.braintribe.model.processing.service.common.ServiceProcessingChain;
import com.braintribe.model.service.api.MulticastRequest;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.platform.service.RxInterceptor;

public class RxConfiguredModelInterceptorTest {

	@Test
	public void keepsLegacyPhaseSemanticsWithoutExplicitOrdering() {
		RxConfiguredModel model = new RxConfiguredModel(null, "test:model");

		List<RxInterceptor> ordered = model.orderedInterceptors(List.of( //
				interceptor("around-1", InterceptorKind.around), //
				interceptor("post-1", InterceptorKind.post), //
				interceptor("pre-1", InterceptorKind.pre), //
				interceptor("post-2", InterceptorKind.post), //
				interceptor("pre-2", InterceptorKind.pre), //
				interceptor("around-2", InterceptorKind.around)));

		assertThat(identifications(ordered)).containsExactly("pre-1", "pre-2", "post-2", "post-1", "around-1", "around-2");
	}

	@Test
	public void explicitOrderingCanMoveAroundInterceptorBeforePreInterceptor() {
		RxConfiguredModel model = new RxConfiguredModel(null, "test:model");
		model.orderInterceptors("audit", "validation");

		List<RxInterceptor> ordered = model.orderedInterceptors(List.of( //
				interceptor("validation", InterceptorKind.pre), //
				interceptor("audit", InterceptorKind.around)));

		assertThat(identifications(ordered)).containsExactly("audit", "validation");
	}

	@Test
	public void orderingDeclarationsAreCumulativeAndCyclesAreRejected() {
		RxConfiguredModel model = new RxConfiguredModel(null, "test:model");
		model.orderInterceptors("a", "b");
		model.orderInterceptors("b", "c");

		assertThat(identifications(model.orderedInterceptors(List.of( //
				interceptor("c", InterceptorKind.around), //
				interceptor("b", InterceptorKind.around), //
				interceptor("a", InterceptorKind.around))))) //
				.containsExactly("a", "b", "c");

		model.orderInterceptors("c", "a");
		assertThatThrownBy(() -> model.orderedInterceptors(List.of( //
				interceptor("a", InterceptorKind.around), //
				interceptor("b", InterceptorKind.around), //
				interceptor("c", InterceptorKind.around)))) //
				.isInstanceOf(IllegalStateException.class) //
				.hasMessageContaining("Cyclic service interceptor ordering");
	}

	@Test
	public void adaptersReproducePreAroundPostExecution() {
		List<String> trace = new ArrayList<>();
		ServiceRequest request = MulticastRequest.T.create();

		ServicePreProcessor<ServiceRequest> pre = (context, incoming) -> {
			trace.add("pre");
			return incoming;
		};
		ServiceAroundProcessor<ServiceRequest, Object> around = (context, incoming, proceed) -> {
			trace.add("around-in");
			Object response = proceed.proceed(incoming);
			trace.add("around-out");
			return response;
		};
		ServicePostProcessor<Object> post = (context, response) -> {
			trace.add("post");
			return response;
		};

		var builder = ServiceProcessingChain.create((context, incoming) -> {
			trace.add("target");
			return "response";
		});
		builder.aroundProcessWith(RxConfiguredModel.asAroundProcessor(pre));
		builder.aroundProcessWith(RxConfiguredModel.asAroundProcessor(post));
		builder.aroundProcessWith(RxConfiguredModel.asAroundProcessor(around));
		Object response = builder.build().process(null, request);

		assertThat(response).isEqualTo("response");
		assertThat(trace).containsExactly("pre", "around-in", "target", "around-out", "post");
	}

	@Test
	public void aroundInterceptorCanObserveFailureFromExplicitlyInnerPreInterceptor() {
		List<String> trace = new ArrayList<>();
		ServiceRequest request = MulticastRequest.T.create();
		RuntimeException validationFailure = new RuntimeException("invalid");

		ServiceAroundProcessor<ServiceRequest, Object> audit = (context, incoming, proceed) -> {
			try {
				return proceed.proceed(incoming);
			} catch (RuntimeException e) {
				trace.add("audited:" + e.getMessage());
				throw e;
			}
		};
		ServicePreProcessor<ServiceRequest> validation = (context, incoming) -> {
			throw validationFailure;
		};

		var builder = ServiceProcessingChain.create((context, incoming) -> "unreachable");
		builder.aroundProcessWith(audit);
		builder.aroundProcessWith(RxConfiguredModel.asAroundProcessor(validation));

		assertThatThrownBy(() -> builder.build().process(null, request)).isSameAs(validationFailure);
		assertThat(trace).containsExactly("audited:invalid");
	}

	private static RxInterceptor interceptor(String identification, InterceptorKind originalKind) {
		ServiceAroundProcessor<ServiceRequest, Object> processor = (context, request, proceed) -> proceed.proceed(request);
		return new RxInterceptor(identification, originalKind, processor, request -> true);
	}

	private static List<String> identifications(List<RxInterceptor> interceptors) {
		return interceptors.stream().map(RxInterceptor::identification).toList();
	}
}

