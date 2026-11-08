package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.log.reflection.model.LogLevelState;

public interface GetLocalLogLevelState extends InternalLogReflectionRequest {
	EntityType<GetLocalLogLevelState> T = EntityTypes.T(GetLocalLogLevelState.class);

	@Override
	EvalContext<LogLevelState> eval(Evaluator<ServiceRequest> evaluator);
}
