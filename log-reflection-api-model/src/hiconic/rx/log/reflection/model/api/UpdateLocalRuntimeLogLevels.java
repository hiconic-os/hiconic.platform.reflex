package hiconic.rx.log.reflection.model.api;

import java.util.Map;
import java.util.Set;

import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

public interface UpdateLocalRuntimeLogLevels extends InternalLogReflectionRequest {
	EntityType<UpdateLocalRuntimeLogLevels> T = EntityTypes.T(UpdateLocalRuntimeLogLevels.class);

	Map<String, String> getLevels();
	void setLevels(Map<String, String> levels);

	Set<String> getNamesToRemove();
	void setNamesToRemove(Set<String> namesToRemove);

	boolean getClearAll();
	void setClearAll(boolean clearAll);

	String getProfile();
	void setProfile(String profile);

	@Override
	EvalContext<Neutral> eval(Evaluator<ServiceRequest> evaluator);
}
