package hiconic.rx.hibernate.model.test;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Person extends GenericEntity {
	EntityType<Person> T = EntityTypes.T(Person.class);
	
	String getName();
	void setName(String name);
	
	String getLastName();
	void setLastName(String lastName);
}
