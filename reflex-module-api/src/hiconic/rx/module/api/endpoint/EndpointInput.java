package hiconic.rx.module.api.endpoint;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.utils.collection.impl.AttributeContexts;

public interface EndpointInput {
	<T extends GenericEntity> T findInput(EntityType<T> inputType);
	
	static EndpointInput get() {
		return AttributeContexts.peek().findOrDefault(EndpointInputAttribute.class, EmptyEndpointInput.INSTANCE);
	}
}
