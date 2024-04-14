package hiconic.rx.model.service.processing.md;


import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Transient;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface HasTransientAssociate extends GenericEntity {
	EntityType<HasTransientAssociate> T = EntityTypes.T(HasTransientAssociate.class);
	
	@Transient
	<A> A getAssociate();
	void setAssociate(Object associate);
}
