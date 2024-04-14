package hiconic.rx.security.model.test;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

@Abstract
public interface SecurityTestRequest extends ServiceRequest {
	EntityType<SecurityTestRequest> T = EntityTypes.T(SecurityTestRequest.class);
}
