package hiconic.rx.hibernate.test;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.BeforeClass;
import org.junit.Test;

import hiconic.rx.hibernate.model.test.Person;
import hiconic.rx.hibernate.test.model.api.GetPersonByName;
import hiconic.rx.hibernate.test.model.api.GetPersons;
import hiconic.rx.hibernate.test.wire.contract.HibernateTestContract;
import hiconic.rx.test.common.AbstractRxTest;

public class HibernateTest extends AbstractRxTest {
	
	@BeforeClass
	public static void onBeforeClass() {
		// System.setProperty("rx.hibernate.module.ormDebugOutputFolder", new File("res/out").getAbsolutePath());
	}
	
	@Test
	public void testHibernate() {
		HibernateTestContract contract = platform.getWireContext().contract(HibernateTestContract.class);
		SessionFactory sessionFactory = contract.mainSessionFactory();
		
		Object id;
		String name = "Hans";
		String lastName = "Wurst";
		
		try (Session session = sessionFactory.openSession()) {
			Transaction transaction = session.beginTransaction();
			Person p = Person.T.create();
			p.setName(name);
			p.setLastName(lastName);
			session.save(p);
			transaction.commit();
			id = p.getId();
		}
		
		try (Session session = sessionFactory.openSession()) {
			Person p = session.createQuery("from Person where id = :id", Person.class).setParameter("id", id).uniqueResult();
			Assertions.assertThat(p).isNotNull();
			Assertions.assertThat(p.getName()).isEqualTo(name);
			Assertions.assertThat(p.getLastName()).isEqualTo(lastName);
		}
		
		GetPersons getPersons = GetPersons.T.create();
		List<Person> list = getPersons.eval(evaluator).get();
		System.out.println(list);
		
		GetPersonByName getPersonByName = GetPersonByName.T.create();
		getPersonByName.setName("Hans");
		
		Person person = getPersonByName.eval(evaluator).get();
		

		Assertions.assertThat(person.getName()).isEqualTo(name);
		

	}
}
