package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

/**
 * Internal cluster payload. Unlike {@link ListLogStreams}, evaluating this request never causes another cluster dispatch.
 */
public interface ListLocalLogStreams extends LogReflectionRequest {
	EntityType<ListLocalLogStreams> T = EntityTypes.T(ListLocalLogStreams.class);

	@Override
	EvalContext<LogStreams> eval(Evaluator<ServiceRequest> evaluator);
}
