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
package hiconic.rx.access.module.processing;

import java.util.function.Function;
import java.util.function.Supplier;

import com.braintribe.cfg.Required;
import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.session.exception.GmSessionException;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.processing.session.api.resource.ResourceAccessFactory;
import com.braintribe.model.processing.session.impl.persistence.BasicPersistenceGmSession;
import com.braintribe.model.processing.session.impl.persistence.auth.BasicSessionAuthorization;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.user.User;
import com.braintribe.model.usersession.UserSession;

import hiconic.rx.module.api.service.ConfiguredModel;

public class RxPersistenceGmSessionFactory implements PersistenceGmSessionFactory {
	private Supplier<AttributeContext> attributeContextSupplier;
	private RxAccesses accesses;
	private Function<AttributeContext, Evaluator<ServiceRequest>> evaluatorSupplier;
	private ResourceAccessFactory<PersistenceGmSession> resourceAccessFactory; 

	@Required
	public void setResourceAccessFactory(ResourceAccessFactory<PersistenceGmSession> resourceAccessFactory) {
		this.resourceAccessFactory = resourceAccessFactory;
	}
	
	@Required
	public void setAccesses(RxAccesses accesses) {
		this.accesses = accesses;
	}

	@Required
	public void setAttributeContextSupplier(Supplier<AttributeContext> attributeContextSupplier) {
		this.attributeContextSupplier = attributeContextSupplier;
	}

	@Required
	public void setEvaluatorSupplier(Function<AttributeContext, Evaluator<ServiceRequest>> evaluatorSupplier) {
		this.evaluatorSupplier = evaluatorSupplier;
	}

	@Override
	public PersistenceGmSession newSession(String accessId) throws GmSessionException {
		RxAccess access = accesses.getAccess(accessId);
		IncrementalAccess incrementalAccess = access.incrementalAccess();
		ConfiguredModel configuredModel = access.configuredDataModel();
		AttributeContext attributeContext = attributeContextSupplier.get();
		CmdResolver cmdResolver = configuredModel.cmdResolver(attributeContext);

		BasicPersistenceGmSession session = new BasicPersistenceGmSession();
		session.setAccessId(accessId);
		session.setIncrementalAccess(incrementalAccess);
		session.setEquivalentSessionFactory(() -> newSession(accessId));
		session.setModelAccessory(new RxModelAccessory(cmdResolver));
		session.setResourcesAccessFactory(resourceAccessFactory);

		session.setRequestEvaluator(evaluatorSupplier.apply(attributeContext));
		session.setDescription("AccessContract PersistenceGmSession accessId=" + accessId + " model=" + configuredModel.name() + " accessType="
				+ access.access().entityType().getTypeSignature());

		UserSession userSession = attributeContext.findOrDefault(UserSessionAspect.class, null);

		if (userSession != null) {
			BasicSessionAuthorization sessionAuthorization = new BasicSessionAuthorization();
			User user = userSession.getUser();

			sessionAuthorization.setSessionId(userSession.getSessionId());
			sessionAuthorization.setUserId(user.getId());
			sessionAuthorization.setUserName(user.getName());
			sessionAuthorization.setUserRoles(userSession.getEffectiveRoles());

			session.setSessionAuthorization(sessionAuthorization);
		}

		return session;
	}

}
