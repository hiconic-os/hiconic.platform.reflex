package hiconic.rx.demo.model.api;

import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

@PositionalArguments("text")
@Description("Reverses the order of letters in the passed text")
public interface ReverseText extends ServiceRequest {
	EntityType<ReverseText> T = EntityTypes.T(ReverseText.class);
	
	String text = "text";
	
	@Description("The text that should be reversed")
	@Mandatory
	String getText();
	void setText(String text);
	
	@Override
	EvalContext<String> eval(Evaluator<ServiceRequest> evaluator);
}
