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

import com.braintribe.model.processing.service.api.ServiceInterceptorProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public class RxInterceptor {

	private final ServiceInterceptorProcessor processor;
	private final Predicate<ServiceRequest> predicate;

	public RxInterceptor(ServiceInterceptorProcessor processor) {
		this(processor, r -> true);
	}

	public RxInterceptor(ServiceInterceptorProcessor processor, Predicate<ServiceRequest> predicate) {
		this.processor = processor;
		this.predicate = predicate;
	}

	public <I extends ServiceInterceptorProcessor> I processor() {
		return (I) processor;
	}

	public Predicate<ServiceRequest> predicate() {
		return predicate;
	}

}
