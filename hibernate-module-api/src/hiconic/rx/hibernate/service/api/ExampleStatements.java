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
package hiconic.rx.hibernate.service.api;

public interface ExampleStatements {
	// empty
}

/*
public interface ExampleStatements {
	@Query("from Person where name = ? or lastName = ?")
	Person foo(String name, String lastName);
}

class MyProcessor implements PersistenceProcessor<ServiceRequest, Object> {

	private ExampleStatements exampleQueries = Persistence.statements(ExampleQueries.class);
	private CrudStatements<Person> sa = Persistence.crud(Person.class);
	
	
	@PersistenceService
	Person getPersonComplex(PersistenceContext context, ServiceRequest request) {
		exampleQueries.foo(name, lastName);
	}
	
	@PersistenceService
	Person getPersonComplex(PersistenceContext context, Session session, ServiceRequest request) {
		Statements.get(session, ExampleQueries.class);
		
		exampleQueries.foo(name, lastName);
	}
}

*/