package hiconic.rx.validation.test;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.path.api.IModelPathElement;
import com.braintribe.model.generic.reflection.EntityType;

@FunctionalInterface
public interface EntityScanListener {

	void visitEntity(GenericEntity entity, EntityType<?> type, IModelPathElement mpe);

}
