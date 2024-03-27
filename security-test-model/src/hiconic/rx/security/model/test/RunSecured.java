package hiconic.rx.security.model.test;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.AuthorizedRequest;

public interface RunSecured extends AuthorizedRequest, SecurityTestRequest {
	EntityType<RunSecured> T = EntityTypes.T(RunSecured.class);
}
