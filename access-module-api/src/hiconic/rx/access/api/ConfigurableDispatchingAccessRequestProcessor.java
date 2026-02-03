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

/**
 * @author peter.gazdik
 */
public class ConfigurableDispatchingAccessRequestProcessor<P extends AccessRequest, R> extends AbstractDispatchingAccessRequestProcessor<P, R> {

	/**
	 * @param dispatcherId
	 *            identification of this processor for (better) error messages.
	 */
	public ConfigurableDispatchingAccessRequestProcessor(String dispatcherId) {
		this.dispatcherId = dispatcherId;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration dispatching) {
		// NO OP
	}

	public <T extends AccessRequest> void register(EntityType<T> denotationType, AccessRequestProcessor<T, ?> processor) {
		dispatchMap.register(denotationType, processor);
	}

	public <T extends AccessRequest> void registerReasoned(EntityType<T> denotationType, ReasonedAccessRequestProcessor<T, ?> processor) {
		dispatchMap.registerReasoned(denotationType, processor);
	}

	public <T extends AccessRequest> void registerStateful(EntityType<T> denotationType,
			Supplier<AbstractStatefulAccessRequestProcessor<T, ?>> processorSupplier) {

		dispatchMap.registerStateful(denotationType, processorSupplier);
	}

	public <T extends AccessRequest> void registerStatefulWithContext(EntityType<T> denotationType,
			Function<AccessRequestContext<T>, AbstractStatefulAccessRequestProcessor<T, ?>> processorFunction) {
		dispatchMap.registerStatefulWithContext(denotationType, processorFunction);
	}

}
