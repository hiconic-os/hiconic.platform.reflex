package hiconic.rx.demo.model.data;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface PersonsAnalysis extends GenericEntity {

	EntityType<PersonsAnalysis> T = EntityTypes.T(PersonsAnalysis.class);

	String averageAge = "averageAge";
	String males = "males";
	String females = "females";

	Integer getAverageAge();
	void setAverageAge(Integer averageAge);

	int getPersonsCount();
	void setPersonsCount(int personsCount);
	
	int getMales();
	void setMales(int males);

	int getFemales();
	void setFemales(int females);

}
