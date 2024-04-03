package hiconic.rx.module.api.endpoint;

import java.util.Collections;
import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

public class EmptyEndpointInput implements EndpointInput {
	public final static EndpointInput INSTANCE = new EmptyEndpointInput();
	@Override
	public <I extends GenericEntity> I findInput(EntityType<I> inputType) {
		return null;
	}
	
	@Override
	public <I extends GenericEntity> List<I> findInputs(EntityType<I> inputType) {
		return Collections.emptyList();
	}
}
