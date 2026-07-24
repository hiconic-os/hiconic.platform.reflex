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
package hiconic.rx.access.smood.module.test;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.hibernate.model.test.Container;
import hiconic.rx.hibernate.model.test.Element;
import hiconic.rx.hibernate.model.test.Person;
import hiconic.rx.test.common.AbstractRxTest;

public class SmoodAccessTest extends AbstractRxTest {

	@Test
	public void storesAndQueriesPolymorphicModelDataAcrossSessions() {
		PersistenceGmSession session = newSession();

		Container container = session.create(Container.T);
		container.setName("Workbench-like tree");
		Element element = session.create(Element.T);
		element.setName("Child");
		container.getElements().add(element);

		Person person = session.create(Person.T);
		person.setName("Ada");
		person.setLastName("Lovelace");
		session.commit();

		Object containerId = container.getId();
		Object personId = person.getId();

		PersistenceGmSession freshSession = newSession();
		Container reloadedContainer = freshSession.query().entity(Container.T, containerId).require();
		Person reloadedPerson = freshSession.query().entity(Person.T, personId).require();

		Assertions.assertThat(reloadedContainer.getElements()).extracting(Element::getName).containsExactly("Child");
		Assertions.assertThat(reloadedPerson.getName()).isEqualTo("Ada");
		Assertions.assertThat(reloadedPerson.getLastName()).isEqualTo("Lovelace");
	}

	private PersistenceGmSession newSession() {
		return resolveExportContract(AccessContract.class).systemSessionFactory().newSession("main-access");
	}

}
