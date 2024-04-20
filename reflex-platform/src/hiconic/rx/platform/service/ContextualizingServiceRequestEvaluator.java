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
