package hiconic.rx.demo.model.api;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.demo.model.data.HasPersons;

public interface GeneratePersons extends PersonRequest {
	EntityType<GeneratePersons> T = EntityTypes.T(GeneratePersons.class);
	
	String personCount = "personCount";
	
	@Initializer("10")
	int getPersonCount();
	void setPersonCount(int personCount);
	
	@Override
	EvalContext<? extends HasPersons> eval(Evaluator<ServiceRequest> evaluator);
}
