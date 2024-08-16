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
