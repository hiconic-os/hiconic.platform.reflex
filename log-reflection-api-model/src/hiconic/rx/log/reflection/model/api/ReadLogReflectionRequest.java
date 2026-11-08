package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.AuthorizedRequest;

/** Public log-reflection operation requiring read access. */
@Abstract
public interface ReadLogReflectionRequest extends LogReflectionRequest, AuthorizedRequest {
	EntityType<ReadLogReflectionRequest> T = EntityTypes.T(ReadLogReflectionRequest.class);
}
