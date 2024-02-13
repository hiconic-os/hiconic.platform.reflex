package hiconic.rx.demo.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

public interface ReverseText extends ServiceRequest {
	EntityType<ReverseText> T = EntityTypes.T(ReverseText.class);
	
	String text = "text";
	
	@Mandatory
	String getText();
	void setText(String text);
	
	@Override
	EvalContext<String> eval(Evaluator<ServiceRequest> evaluator);
}
