package hiconic.rx.demo.app.processing;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.impl.AbstractDispatchingServiceProcessor;
import com.braintribe.model.processing.service.impl.DispatchConfiguration;

import hiconic.rx.demo.model.api.AnalyzePersons;
import hiconic.rx.demo.model.api.GeneratePersons;
import hiconic.rx.demo.model.api.PersonRequest;
import hiconic.rx.demo.model.data.Gender;
import hiconic.rx.demo.model.data.HasPersons;
import hiconic.rx.demo.model.data.Person;
import hiconic.rx.demo.model.data.PersonsAnalysis;

/**
 * 
 * Example curl call to generate person data into a file call result.json
 * curl http://localhost:8080/api/main/GeneratePersons?personCount=1000000 > result.json
 */
public class PersonRequestProcessor extends AbstractDispatchingServiceProcessor<PersonRequest, Object> {
	private Supplier<DataGenerationSource> dataGenerationSourceSupplier;
	
	@Required
	public void setDataGenerationSourceSupplier(Supplier<DataGenerationSource> dataGenerationSourceSupplier) {
		this.dataGenerationSourceSupplier = dataGenerationSourceSupplier;
	}
	
	@Override
	protected void configureDispatching(DispatchConfiguration<PersonRequest, Object> dispatching) {
		dispatching.registerReasoned(GeneratePersons.T, this::generatePersons);
		dispatching.registerReasoned(AnalyzePersons.T, this::analyzePersons);
	}
	
	private Maybe<HasPersons> generatePersons(ServiceRequestContext context, GeneratePersons request) {

		HasPersons result = HasPersons.T.create();
		
		int count = request.getPersonCount();
		
		DataGenerationSource source = dataGenerationSourceSupplier.get();
		
		List<String> names = source.getNames();
		List<String> lastNames = source.getLastNames();

		Random random = new Random(0);
		
		int namesSize = names.size();
		int lastNamesSize = lastNames.size();
		Gender[] genders = Gender.values();
		long thirtyYearsInMs = 30L * 365 * 24 * 60 * 60 * 1000;
		List<Person> persons = result.getPersons();
		
		for (int i = 0; i < count; i++) {
			String name = names.get(random.nextInt(namesSize));
			String lastName = lastNames.get(random.nextInt(lastNamesSize));
			String email = name.toLowerCase() + "." + lastName.toLowerCase() + "@demo.rx";
			Gender gender = genders[random.nextInt(genders.length)];
			Date birthday = new Date(random.nextLong(thirtyYearsInMs)); 
			
			Person p = Person.T.create();
			p.setName(name);
			p.setLastName(lastName);
			p.setEmail(email);
			p.setGender(gender);
			p.setBirthday(birthday);
			
			persons.add(p);
		}
		
		return Maybe.complete(result);
	}
	
	private Maybe<PersonsAnalysis> analyzePersons(ServiceRequestContext context, AnalyzePersons request) {
		
		return Maybe.complete(null);
	}
}
