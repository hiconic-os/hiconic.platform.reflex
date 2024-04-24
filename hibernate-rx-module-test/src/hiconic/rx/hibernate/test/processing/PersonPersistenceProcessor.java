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
		
		List<Person> list = session.createQuery("from Person", Person.class).list();
		
		return Maybe.complete(list);
		
	}

}
