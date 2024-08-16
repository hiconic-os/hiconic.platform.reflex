package hiconic.rx.hibernate.model.test;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Element extends GenericEntity {
	EntityType<Element> T = EntityTypes.T(Element.class);
	
	String getName();
	void setName(String name);

}
