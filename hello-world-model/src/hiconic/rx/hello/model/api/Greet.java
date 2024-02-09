package hiconic.rx.hello.model.api;


import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

public interface Greet extends ServiceRequest {
	EntityType<Greet> T = EntityTypes.T(Greet.class);
	
	String name = "name";
	
	String getName();
	void setName(String name);
	
	@Override
	EvalContext<String> eval(Evaluator<ServiceRequest> evaluator);
}
