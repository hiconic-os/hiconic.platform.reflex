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

import java.util.ArrayList;
import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

/**
 * The platform-wide fallback processor plus an exact reflection of the request types for which a fallback was registered.
 * The domain dispatcher uses this information to distinguish supported fallback requests from genuinely unmapped requests.
 */
public class FallbackServiceProcessor extends ConfigurableDispatchingServiceProcessor {

	private final List<EntityType<? extends ServiceRequest>> requestTypes = new ArrayList<>();

	@Override
	public <R extends ServiceRequest> void register(EntityType<R> requestType, ServiceProcessor<? super R, ?> serviceProcessor) {
		super.register(requestType, serviceProcessor);
		requestTypes.add(requestType);
	}

	public boolean supports(ServiceRequest request) {
		return requestTypes.stream().anyMatch(type -> type.isInstance(request));
	}
}
