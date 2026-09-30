package hiconic.rx.platform.service.model;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.DomainRequest;

/** Test request whose processor verifies the complete system identity in the request context. */
public interface CheckSystemUserAttributes extends DomainRequest {
	EntityType<CheckSystemUserAttributes> T = EntityTypes.T(CheckSystemUserAttributes.class);
}
