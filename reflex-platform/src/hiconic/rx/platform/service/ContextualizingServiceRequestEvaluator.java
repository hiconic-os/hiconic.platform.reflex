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

import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.service.api.ParentAttributeContextAspect;
import com.braintribe.model.service.api.ServiceRequest;

public class ContextualizingServiceRequestEvaluator implements Evaluator<ServiceRequest> {

	private Evaluator<ServiceRequest> delegate;
	private Supplier<AttributeContext> attributeContext;
	
	@Required
	public void setAttributeContextProvider(Supplier<AttributeContext> attributeContext) {
		this.attributeContext = attributeContext;
	}
	
	@Required
	public void setDelegate(Evaluator<ServiceRequest> delegate) {
		this.delegate = delegate;
	}
	
	@Override
	public <T> EvalContext<T> eval(ServiceRequest evaluable) {
		EvalContext<T> evalContext = delegate.<T>eval(evaluable);
		evalContext.setAttribute(ParentAttributeContextAspect.class, attributeContext.get());
		return evalContext;
	}
}
