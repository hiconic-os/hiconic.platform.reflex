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

import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.model.md.InterceptAccessWith;
import hiconic.rx.access.smood.module.test.context.AttributeContextMarker;
import hiconic.rx.access.smood.module.test.context.AttributeContextProbeAspect;
import hiconic.rx.access.smood.module.test.context.ReadAttributeContextMarker;
import hiconic.rx.access.smood.module.test.context.ReadAttributeContextIdentity;
import hiconic.rx.access.smood.module.test.wire.contract.SmoodAccessContextTestContract;
import hiconic.rx.hibernate.model.test.Container;
import hiconic.rx.hibernate.model.test.Element;
import hiconic.rx.hibernate.model.test.Person;
import hiconic.rx.test.common.AbstractRxTest;

public class SmoodAccessTest extends AbstractRxTest {
	private static final String DERIVED_MARKER = "derived-invocation";

	@Test
	public void sessionEvaluationUsesCurrentDerivedAttributeContext() {
		PersistenceGmSession session = contextSession();

		String observed = withMarker(() -> evaluateMarker(session));

		Assertions.assertThat(observed).isEqualTo(DERIVED_MARKER);
	}

	@Test
	public void equivalentSessionEvaluationUsesCurrentDerivedAttributeContext() {
		PersistenceGmSession equivalentSession = contextSession().newEquivalentSession();

		String observed = withMarker(() -> evaluateMarker(equivalentSession));

		Assertions.assertThat(observed).isEqualTo(DERIVED_MARKER);
	}

	@Test
	public void sessionEvaluationCombinesCurrentContextWithSessionIdentity() {
		assertEvaluationSeesCurrentContextAndSessionIdentity(false);
	}

	@Test
	public void equivalentSessionEvaluationCombinesCurrentContextWithSessionIdentity() {
		assertEvaluationSeesCurrentContextAndSessionIdentity(true);
	}

	@Test
	public void accessInvocationUsesCurrentDerivedAttributeContext() {
		assertAccessSeesCurrentContext(contextSession());
	}

	@Test
	public void equivalentSessionAccessUsesCurrentDerivedAttributeContext() {
		assertAccessSeesCurrentContext(contextSession().newEquivalentSession());
	}

	@Test
	public void accessModelsRemainOpenWithoutSecurityFeature() {
		AccessContract access = resolveExportContract(AccessContract.class);
		var model = access.accessDomains().byId("main-access").configuredDataModel();

		Assertions.assertThat(model.systemCmdResolver().getMetaData().meta(InterceptAccessWith.T).list())
				.extracting(InterceptAccessWith::getAssociate)
				.containsOnly(resolveExportContract(SmoodAccessContextTestContract.class).attributeContextProbeAspect());

		PersistenceGmSession systemSession = access.systemSessionFactory().newSession("main-access");
		Person person = systemSession.create(Person.T);
		person.setName("Open");
		systemSession.commit();

		User user = User.T.create();
		user.setId("regular-user");
		user.setName("regular-user");
		UserSession userSession = UserSession.T.create();
		userSession.setUser(user);
		userSession.getEffectiveRoles().add("regular-user");

		var people = AttributeContexts.derivePeek().set(UserSessionAspect.class, userSession).buildAnd().execute(() -> access
				.contextSessionFactory().newSession("main-access").query().entities(EntityQueryBuilder.from(Person.T).done()).list());
		Assertions.assertThat(people).hasSize(1);
	}

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

	private PersistenceGmSession contextSession() {
		return resolveExportContract(AccessContract.class).contextSessionFactory().newSession("main-access");
	}

	private String evaluateMarker(PersistenceGmSession session) {
		ReadAttributeContextMarker request = ReadAttributeContextMarker.T.create();
		request.setDomainId("main-access");
		return (String) request.eval(session).get();
	}

	private void assertEvaluationSeesCurrentContextAndSessionIdentity(boolean equivalent) {
		User user = User.T.create();
		user.setId("session-user");
		user.setName("session-user");
		UserSession userSession = UserSession.T.create();
		userSession.setSessionId("session-id");
		userSession.setUser(user);
		userSession.getEffectiveRoles().add("session-role");

		PersistenceGmSession session = AttributeContexts.derivePeek().set(UserSessionAspect.class, userSession).buildAnd()
				.execute(this::contextSession);
		if (equivalent)
			session = session.newEquivalentSession();

		PersistenceGmSession evaluatedSession = session;
		String observed = withMarker(() -> {
			ReadAttributeContextIdentity request = ReadAttributeContextIdentity.T.create();
			request.setDomainId("main-access");
			return (String) request.eval(evaluatedSession).get();
		});

		Assertions.assertThat(observed).isEqualTo(DERIVED_MARKER + ":session-user");
	}

	private void assertAccessSeesCurrentContext(PersistenceGmSession session) {
		AttributeContextProbeAspect probe = resolveExportContract(SmoodAccessContextTestContract.class).attributeContextProbeAspect();
		probe.reset();

		withMarker(() -> session.query().entities(EntityQueryBuilder.from(Person.T).done()).list());

		Assertions.assertThat(probe.observedMarker()).isEqualTo(DERIVED_MARKER);
	}

	private <T> T withMarker(java.util.function.Supplier<T> action) {
		return AttributeContexts.derivePeek().set(AttributeContextMarker.class, DERIVED_MARKER).buildAnd().execute(action::get);
	}

}
