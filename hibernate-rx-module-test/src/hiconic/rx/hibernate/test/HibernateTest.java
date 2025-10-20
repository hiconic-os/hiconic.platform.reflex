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
package hiconic.rx.hibernate.test;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.BeforeClass;
import org.junit.Test;

import hiconic.rx.hibernate.model.test.Container;
import hiconic.rx.hibernate.model.test.Element;
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
	public void testIsolatedFetch() {
		HibernateTestContract contract = platform.getWireContext().contract(HibernateTestContract.class);
		SessionFactory sessionFactory = contract.mainSessionFactory();

		Object id;
		String name = "Owner";

		try (Session session = sessionFactory.openSession()) {
			Transaction transaction = session.beginTransaction();
			Container c = Container.T.create();
			c.setName(name);
			session.save(c);

			Element e1 = Element.T.create();
			Element e2 = Element.T.create();
			Element e3 = Element.T.create();

			e1.setName("Element 1");
			e2.setName("Element 2");
			e3.setName("Element 3");

			session.save(e1);
			session.save(e2);
			session.save(e3);

			c.getElements().add(e1);
			c.getElements().add(e2);
			c.getElements().add(e3);

			transaction.commit();

			id = c.getId();
		}

		try (Session session = sessionFactory.openSession()) {
			Container c = session.createQuery("from Container where id = :id", Container.class).setParameter("id", id).uniqueResult();
			List<Element> fetchedElements = session.createQuery("select e from Container c join c.elements e where c.id = :id", Element.class) //
					.setParameter("id", id) //
					.list();

			session.setReadOnly(c, true);
			List<Element> elements = c.getElements();
			elements.clear();
			elements.addAll(fetchedElements);
			session.setReadOnly(c, false);
		}
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
