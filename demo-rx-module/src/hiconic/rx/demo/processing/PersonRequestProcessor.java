// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.demo.processing;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.processing.service.api.ServiceProcessor;
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
 * {@link ServiceProcessor} for {@link PersonRequest}s.
 *
 * <h2>Curl examples</h2>
 * 
 * Generate person data:
 * 
 * <pre>
 * curl "http://localhost:8080/api/main/GeneratePersons?personCount=1000000&type-explicitness=never" > persons.json
 * </pre>
 * 
 * Analyze person data:
 * 
 * <pre>
 * curl "http://localhost:8080/api/main/AnalyzePersons?TODO"
 * </pre>
 */
public class PersonRequestProcessor extends AbstractDispatchingServiceProcessor<PersonRequest, Object> {

	private static long YEAR_IN_MS = 365L * 24 * 60 * 60 * 1000;
	private static long THIRTY_YEARS_IN_MS = 30L * YEAR_IN_MS;

	private Supplier<DataGenerationSource> dataGenerationSourceSupplier;

	@Required
	public void setDataGenerationSourceSupplier(Supplier<DataGenerationSource> dataGenerationSourceSupplier) {
		this.dataGenerationSourceSupplier = dataGenerationSourceSupplier;
	}

	@Override
	protected void configureDispatching(DispatchConfiguration<PersonRequest, Object> dispatching) {
		dispatching.registerReasoned(GeneratePersons.T, (ctx, req) -> generatePersons(req));
		dispatching.registerReasoned(AnalyzePersons.T, (ctx, req) -> analyzePersons(req));
	}

	//
	// GeneratePersons
	//
	private Maybe<HasPersons> generatePersons(GeneratePersons request) {
		DataGenerator generator = new DataGenerator();
		int count = request.getPersonCount();

		HasPersons result = HasPersons.T.create();

		List<Person> persons = result.getPersons();
		for (int i = 0; i < count; i++)
			persons.add(generatePerson(generator));
		
		return Maybe.complete(result);
	}

	private Person generatePerson(DataGenerator generator) {
		Person p = Person.T.create();

		p.setName(generator.randomName());
		p.setLastName(generator.randomLastName());
		p.setGender(generator.randomGander());
		p.setBirthday(generator.randomBirthDate());

		p.setEmail((p.getName() + "." + p.getLastName() + "@demo.rx").toLowerCase());

		return p;
	}

	class DataGenerator {
		private final DataGenerationSource source = dataGenerationSourceSupplier.get();

		private final List<String> names = source.getNames();
		private final List<String> lastNames = source.getLastNames();

		private final Random random = new Random(0);

		private final int namesSize = names.size();
		private final int lastNamesSize = lastNames.size();

		public String randomName() {
			return names.get(random.nextInt(namesSize));
		}

		public String randomLastName() {
			return lastNames.get(random.nextInt(lastNamesSize));
		}

		public Gender randomGander() {
			return random.nextBoolean() ? Gender.male : Gender.female;
		}

		public Date randomBirthDate() {
			return new Date(random.nextLong(THIRTY_YEARS_IN_MS));
		}
	}

	//
	// AnalyzePersons
	//
	private Maybe<PersonsAnalysis> analyzePersons(AnalyzePersons request) {
		return Maybe.complete(analyze(request.getPersons()));
	}

	private PersonsAnalysis analyze(List<Person> persons) {
		PersonsAnalysis result = PersonsAnalysis.T.create();

		if (persons.isEmpty()) {
			return result;
		}

		long ageSum = 0;
		int males = 0;
		int females = 0;
		int unknowns = 0;

		for (Person p : persons) {
			ageSum += p.getBirthday().getTime();

			if (p.getGender() == Gender.male) {
				males++;

			} else if (p.getGender() == Gender.female) {
				females++;

			} else {
				unknowns++;
			}
		}

		int total = males + females + unknowns;
		long avgAgeTime = ageSum / total;
		long avgAgeYears = avgAgeTime / YEAR_IN_MS - 1970; // leap years aren't real

		result.setAverageAge((int) avgAgeYears);
		result.setPersonsCount(total);
		result.setMales(males);
		result.setFemales(females);

		return result;
	}
}
