package hiconic.rx.module.api.endpoint;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

public class EmptyEndpointInput implements EndpointInput {

	public static final EmptyEndpointInput INSTANCE = new EmptyEndpointInput();
	
	@Override
	public <T extends GenericEntity> T findInput(EntityType<T> inputType) {
		return null;
	}
}
