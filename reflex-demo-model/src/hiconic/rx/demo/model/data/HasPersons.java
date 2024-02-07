package hiconic.rx.demo.model.data;

import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface HasPersons extends GenericEntity {

	EntityType<HasPersons> T = EntityTypes.T(HasPersons.class);

	String persons = "persons";

	List<Person> getPersons();
	void setPersons(List<Person> persons);

}
