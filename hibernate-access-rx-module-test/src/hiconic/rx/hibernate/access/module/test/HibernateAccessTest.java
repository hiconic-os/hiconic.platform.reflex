package hiconic.rx.hibernate.access.module.test;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.hibernate.model.test.Person;
import hiconic.rx.test.common.AbstractRxTest;

public class HibernateAccessTest extends AbstractRxTest {
	
	@BeforeClass
	public static void onBeforeClass() {
		System.setProperty("rx.hibernate.module.ormDebugOutputFolder", new File("res/out").getAbsolutePath());
	}
	
	@Test
	public void testHibernateAccess() {
		PersistenceGmSession session = newSession();
		
		Object id;
		String name = "Hans";
		String lastName = "Wurst";
		
		Person p = session.create(Person.T);
		p.setName(name);
		p.setLastName(lastName);
		session.commit();
		
		id = p.getId();
		
		session = newSession(); 
		
		p = session.query().entity(Person.T, id).require();
		
		Assertions.assertThat(p).isNotNull();
		Assertions.assertThat(p.getName()).isEqualTo(name);
		Assertions.assertThat(p.getLastName()).isEqualTo(lastName);
	}

	private PersistenceGmSession newSession() {
		AccessContract contract = platform.getWireContext().contract(AccessContract.class);
		return contract.systemSessionFactory().newSession("main-access");
	}
}
