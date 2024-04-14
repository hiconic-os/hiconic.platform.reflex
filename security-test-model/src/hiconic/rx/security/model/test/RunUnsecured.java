package hiconic.rx.security.model.test;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface RunUnsecured extends SecurityTestRequest {
	EntityType<RunUnsecured> T = EntityTypes.T(RunUnsecured.class);
}
