package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

/** Reflects the applications and nodes currently visible to cluster routing. */
public interface GetLogTopology extends LogReflectionRequest {
	EntityType<GetLogTopology> T = EntityTypes.T(GetLogTopology.class);

	@Override
	EvalContext<LogTopology> eval(Evaluator<ServiceRequest> evaluator);
}
