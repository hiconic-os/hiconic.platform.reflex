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

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;
import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.core.expert.impl.PolymorphicDenotationMap;

public abstract class AbstractDispatchingAccessRequestProcessor<P extends AccessRequest, R> implements ReasonedAccessRequestProcessor<P, R> {

	protected final DispatchMap dispatchMap = new DispatchMap();

	/** Optional identification of this dispatcher for better error messages. */
	protected String dispatcherId;

	protected AbstractDispatchingAccessRequestProcessor() {
		configureDispatching(dispatchMap);
	}

	protected abstract void configureDispatching(DispatchConfiguration dispatching);

	@Override
	public Maybe<? extends R> processReasoned(AccessRequestContext<P> context) {
		P request = context.getOriginalRequest();

		ReasonedAccessRequestProcessor<P, R> processor = dispatchMap.get(request);
		if (processor == null)
			return Reasons.build(UnsupportedOperation.T) //
					.text("No processor configured for access request " + request.entityType().getTypeSignature() + "." + optionalDispatchId()) //
					.toMaybe();

		return processor.processReasoned(context);
	}

	private String optionalDispatchId() {
		return dispatcherId == null ? "" : " Dispatcher: " + dispatcherId;
	}

	protected static class DispatchMap //
			extends PolymorphicDenotationMap<AccessRequest, AccessRequestProcessor<?, ?>> //
			implements DispatchConfiguration {

		@Override
		public <T extends AccessRequest> void register(EntityType<T> denotationType, AccessRequestProcessor<T, ?> processor) {
			put(denotationType, processor);
		}
	}

}
