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
package hiconic.rx.access.api;

import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.generic.reflection.EntityType;

public interface DispatchConfiguration {
	
	<T extends AccessRequest> void register(EntityType<T> denotationType, AccessRequestProcessor<T, ?> processor);
	
	default <T extends AccessRequest> void registerReasoned(EntityType<T> denotationType, ReasonedAccessRequestProcessor<T, ?> processor) {
		register(denotationType, processor);
	}
	
	/**
	 * @deprecated use {@link #registerStateful(EntityType, Supplier)} 
	 */
	@Deprecated
	default <T extends AccessRequest> void register(EntityType<T> denotationType, Supplier<AbstractStatefulAccessRequestProcessor<T, ?>> processorSupplier) {
		registerStateful(denotationType, processorSupplier);
	}
	
	default <T extends AccessRequest> void registerStateful(EntityType<T> denotationType, Supplier<AbstractStatefulAccessRequestProcessor<T, ?>> processorSupplier) {
		register(denotationType, c -> {
			AbstractStatefulAccessRequestProcessor<T, ?> processor = processorSupplier.get();
			processor.initContext(c);
			return processor.process();
		});
	}
	
	default <T extends AccessRequest> void registerStatefulWithContext(EntityType<T> denotationType, Function<AccessRequestContext<T>, AbstractStatefulAccessRequestProcessor<T, ?>> processorFunction) {
		register(denotationType, c -> {
			AbstractStatefulAccessRequestProcessor<T, ?> processor = processorFunction.apply(c);
			processor.initContext(c);
			return processor.process();
		});
	}
	
}
