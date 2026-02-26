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
package hiconic.rx.rest.module.test;

import static com.braintribe.testing.junit.assertions.gm.assertj.core.api.GmAssertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.junit.Test;

import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.hibernate.model.test.Person;
import hiconic.rx.test.common.AbstractRxTest;
import hiconic.rx.web.server.api.WebServerContract;

public class RestTest extends AbstractRxTest {
	
	private int getPort() {
		WebServerContract webServer = platform.getWireContext().contract(WebServerContract.class);
		return webServer.getEffectiveServerPort();
	}
	
	@Test
	public void testGet() throws Exception {
		generateData();
		
		HttpClient httpClient = HttpClient.newBuilder().build();
		URI uri = URI.create("http://localhost:" + getPort() + "/rest/entities/main-access/Person");
		HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
		HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
		
		String body = response.body();
		
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(body) //
				.contains(Person.T.getTypeSignature()) //
				.contains("Hans") //
		;

		// System.out.println(body);
	}
	
	private void generateData() {
		PersistenceGmSession session = newSession();
		
		String name = "Hans";
		String lastName = "Wurst";
		
		Person p = session.create(Person.T);
		p.setName(name);
		p.setLastName(lastName);
		session.commit();
	}
	
	
	private PersistenceGmSession newSession() {
		AccessContract contract = platform.getWireContext().contract(AccessContract.class);
		return contract.systemSessionFactory().newSession("main-access");
	}
}
