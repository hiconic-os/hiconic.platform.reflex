package hiconic.rx.hibernate.test.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.model.test.Person;

public interface GetPersonByName extends ServiceRequest {
	EntityType<GetPersonByName> T = EntityTypes.T(GetPersonByName.class);

	String getName();
	void setName(String name);
	
	@Override
	EvalContext<Person> eval(Evaluator<ServiceRequest> evaluator);
}
