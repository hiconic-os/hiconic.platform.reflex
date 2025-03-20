package hiconic.rx.cli.processing.help.model;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

/**
 * @author peter.gazdik
 */
public interface ComRef_SimpleRequest extends ServiceRequest {

	EntityType<ComRef_SimpleRequest> T = EntityTypes.T(ComRef_SimpleRequest.class);

	String getName();
	void setName(String name);

	@Override
	EvalContext<String> eval(Evaluator<ServiceRequest> evaluator);

}
