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
package hiconic.rx.access.module.processing;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.api.ParentAttributeContextAspect;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.usersession.UserSession;

/**
 * Applies a persistence session's stable identity to service and resource requests while preserving all attributes
 * contributed by the current invocation.
 */
final class SessionIdentityServiceRequestEvaluator implements Evaluator<ServiceRequest> {
	private final Evaluator<ServiceRequest> delegate;
	private final UserSession userSession;

	SessionIdentityServiceRequestEvaluator(Evaluator<ServiceRequest> delegate, UserSession userSession) {
		this.delegate = delegate;
		this.userSession = userSession;
	}

	@Override
	public <T> EvalContext<T> eval(ServiceRequest evaluable) {
		EvalContext<T> evalContext = delegate.eval(evaluable);
		if (userSession != null)
			evalContext.setAttribute(ParentAttributeContextAspect.class, SessionIdentityContext.derive(userSession));
		return evalContext;
	}
}
