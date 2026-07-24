package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.AuthorizedRequest;

@Abstract
public interface LogReflectionRequest extends AuthorizedRequest {
	EntityType<LogReflectionRequest> T = EntityTypes.T(LogReflectionRequest.class);
}
