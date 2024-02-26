package hiconic.rx.platform.cli.model.api;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.DomainRequest;
import com.braintribe.model.service.api.ServiceRequest;

@Abstract
public interface CliRequest extends ServiceRequest, DomainRequest {
	EntityType<CliRequest> T = EntityTypes.T(CliRequest.class);

	
	@Initializer("'cli'")
	@Override
	String getDomainId();
}
