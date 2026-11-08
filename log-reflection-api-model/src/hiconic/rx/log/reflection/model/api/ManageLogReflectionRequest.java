package hiconic.rx.log.reflection.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.AuthorizedRequest;

/** Public log-reflection operation changing logging configuration. */
@Abstract
public interface ManageLogReflectionRequest extends LogReflectionRequest, AuthorizedRequest {
	EntityType<ManageLogReflectionRequest> T = EntityTypes.T(ManageLogReflectionRequest.class);
}
