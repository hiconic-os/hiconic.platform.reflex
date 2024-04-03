package hiconic.rx.module.api.endpoint;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.utils.collection.impl.AttributeContexts;

public interface EndpointInput {
	
	<I extends GenericEntity> I findInput(EntityType<I> inputType);
	<I extends GenericEntity> List<I> findInputs(EntityType<I> inputType);
	
	
	static EndpointInput get() {
		return AttributeContexts.peek().findOrDefault(EndpointInputAttribute.class, EmptyEndpointInput.INSTANCE);
	}
}
