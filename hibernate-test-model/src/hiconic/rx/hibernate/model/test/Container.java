package hiconic.rx.hibernate.model.test;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Container extends GenericEntity {
	EntityType<Container> T = EntityTypes.T(Container.class);
	
	String getName();
	void setName(String name);
	
	List<Element> getElements();
	void setElements(List<Element> elements);
}
