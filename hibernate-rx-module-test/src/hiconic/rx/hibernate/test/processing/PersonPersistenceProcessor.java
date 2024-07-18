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
package hiconic.rx.hibernate.test.processing;

import java.util.List;

import org.hibernate.Session;

import com.braintribe.gm.model.reason.Maybe;

import hiconic.rx.hibernate.annotations.PersistenceService;
import hiconic.rx.hibernate.annotations.TransactionMode;
import hiconic.rx.hibernate.model.test.Person;
import hiconic.rx.hibernate.service.api.PersistenceContext;
import hiconic.rx.hibernate.service.api.PersistenceProcessor;
import hiconic.rx.hibernate.test.model.api.GetPersons;

public class PersonPersistenceProcessor implements PersistenceProcessor<GetPersons, Object> {

	@PersistenceService(TransactionMode.READ)
	public Maybe<List<Person>> getPersons(PersistenceContext context, Session session, GetPersons request) {
		
		List<Person> list = session.createQuery("from Person p fetch join p.addresses", Person.class).list();
		
		return Maybe.complete(list);
		
	}

}
