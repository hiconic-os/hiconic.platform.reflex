package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.log.reflection.model.LogTarget;

public interface ListLogStreams extends LogReflectionRequest {
	EntityType<ListLogStreams> T = EntityTypes.T(ListLogStreams.class);

	LogTarget getTarget();
	void setTarget(LogTarget target);

	@Override
	EvalContext<LogStreams> eval(Evaluator<ServiceRequest> evaluator);
}
