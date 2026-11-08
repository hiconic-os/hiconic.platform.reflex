package hiconic.rx.log.reflection.model.api;

import java.util.List;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

public interface UpdateLocalLogLevelProfileCascade extends InternalLogReflectionRequest {
	EntityType<UpdateLocalLogLevelProfileCascade> T = EntityTypes.T(UpdateLocalLogLevelProfileCascade.class);

	List<String> getProfiles();
	void setProfiles(List<String> profiles);

	boolean getReset();
	void setReset(boolean reset);

	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);
}
