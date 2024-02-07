package hiconic.rx.demo.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.demo.model.data.HasPersons;
import hiconic.rx.demo.model.data.PersonsAnalysis;

public interface AnalyzePersons extends PersonRequest, HasPersons {

	EntityType<AnalyzePersons> T = EntityTypes.T(AnalyzePersons.class);

	@Override
	EvalContext<? extends PersonsAnalysis> eval(Evaluator<ServiceRequest> evaluator);

}
