package hiconic.rx.hibernate.test.model.api;

import java.util.List;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.model.test.Person;

public interface GetPersons extends ServiceRequest {
	EntityType<GetPersons> T = EntityTypes.T(GetPersons.class);

	@Override
	EvalContext<List<Person>> eval(Evaluator<ServiceRequest> evaluator);
}
