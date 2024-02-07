package hiconic.rx.demo.model.data;

import java.util.Date;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface PersonsAnalysis extends GenericEntity {
	EntityType<PersonsAnalysis> T = EntityTypes.T(PersonsAnalysis.class);
	
}
