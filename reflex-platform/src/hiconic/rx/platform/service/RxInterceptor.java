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
package hiconic.rx.platform.service;

import java.util.function.Predicate;

import com.braintribe.model.processing.service.api.InterceptorKind;
import com.braintribe.model.processing.service.api.ServiceAroundProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public class RxInterceptor {

	private final ServiceAroundProcessor<?, ?> processor;
	private final Predicate<ServiceRequest> predicate;
	private final String identification;
	private final InterceptorKind originalKind;

	public RxInterceptor(ServiceAroundProcessor<?, ?> processor) {
		this(null, InterceptorKind.around, processor, r -> true);
	}

	public RxInterceptor(String identification, InterceptorKind originalKind, ServiceAroundProcessor<?, ?> processor,
			Predicate<ServiceRequest> predicate) {
		this.identification = identification;
		this.originalKind = originalKind;
		this.processor = processor;
		this.predicate = predicate;
	}

	public String identification() {
		return identification;
	}

	public InterceptorKind originalKind() {
		return originalKind;
	}

	public <I extends ServiceAroundProcessor<?, ?>> I processor() {
		return (I) processor;
	}

	public Predicate<ServiceRequest> predicate() {
		return predicate;
	}

}
