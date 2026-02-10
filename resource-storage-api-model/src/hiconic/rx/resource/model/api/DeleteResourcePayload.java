// ============================================================================
package hiconic.rx.resource.model.api;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

public interface DeleteResourcePayload extends ExistingResourcePayloadRequest {

	EntityType<DeleteResourcePayload> T = EntityTypes.T(DeleteResourcePayload.class);

	@Override
	EvalContext<DeleteResourcePayloadResponse> eval(Evaluator<ServiceRequest> evaluator);

}
