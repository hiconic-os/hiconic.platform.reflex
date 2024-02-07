package hiconic.rx.demo.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

@Abstract
public interface PersonRequest extends ServiceRequest {
	EntityType<PersonRequest> T = EntityTypes.T(PersonRequest.class);
}
