package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.AuthorizedRequest;

/** Cluster-internal operation which must never be authorized through user roles. */
@Abstract
public interface InternalLogReflectionRequest extends LogReflectionRequest, AuthorizedRequest {
	EntityType<InternalLogReflectionRequest> T = EntityTypes.T(InternalLogReflectionRequest.class);
}
