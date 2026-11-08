package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

public interface ApplyEffectiveLogLevels extends InternalLogReflectionRequest {
	EntityType<ApplyEffectiveLogLevels> T = EntityTypes.T(ApplyEffectiveLogLevels.class);

	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);
}
