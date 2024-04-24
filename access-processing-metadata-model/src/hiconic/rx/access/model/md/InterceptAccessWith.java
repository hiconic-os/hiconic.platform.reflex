package hiconic.rx.access.model.md;


import com.braintribe.model.generic.annotation.Transient;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.ModelMetaData;

public interface InterceptAccessWith extends ModelMetaData {
	EntityType<InterceptAccessWith> T = EntityTypes.T(InterceptAccessWith.class);
	
	@Transient
	<A> A getAssociate();
	void setAssociate(Object associate);
	
	static InterceptAccessWith create(Object accessAspect) {
		InterceptAccessWith interceptAccessWith = InterceptAccessWith.T.create();
		interceptAccessWith.setAssociate(accessAspect);
		return interceptAccessWith;
	}
}
