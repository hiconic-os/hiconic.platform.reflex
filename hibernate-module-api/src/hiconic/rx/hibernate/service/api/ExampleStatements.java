package hiconic.rx.hibernate.service.api;

import org.hibernate.Session;

import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.annotations.PersistenceService;


public interface ExampleStatements {}

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