package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.AuthorizableRequest;

@Abstract
public interface LogReflectionRequest extends AuthorizableRequest {
	EntityType<LogReflectionRequest> T = EntityTypes.T(LogReflectionRequest.class);
}
