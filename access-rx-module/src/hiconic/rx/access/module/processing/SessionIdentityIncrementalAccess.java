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

import java.util.Set;

import com.braintribe.common.attribute.AttributeContextBuilder;
import com.braintribe.common.attribute.common.UserInfo;
import com.braintribe.common.attribute.common.UserInfoAttribute;
import com.braintribe.model.access.AbstractDelegatingAccess;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.access.ModelAccessException;
import com.braintribe.model.accessapi.CustomPersistenceRequest;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.accessapi.ReferencesRequest;
import com.braintribe.model.accessapi.ReferencesResponse;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.securityservice.api.attributes.LenientAuthenticationFailure;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.aspect.IsAuthorizedAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorSessionIdAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorUserNameAspect;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.query.EntityQueryResult;
import com.braintribe.model.query.PropertyQuery;
import com.braintribe.model.query.PropertyQueryResult;
import com.braintribe.model.query.SelectQuery;
import com.braintribe.model.query.SelectQueryResult;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

/**
 * Makes the stable identity of a persistence session visible to nested access processing without
 * freezing the complete attribute context that happened to exist when the session was created.
 * Every invocation derives from the current context, so attributes introduced by the current
 * invocation remain visible. In particular this also applies to equivalent sessions.
 */
public class SessionIdentityIncrementalAccess extends AbstractDelegatingAccess {

	private final UserSession userSession;

	public SessionIdentityIncrementalAccess(IncrementalAccess delegate, UserSession userSession) {
		setDelegate(delegate);
		this.userSession = userSession;
	}

	@Override
	public GmMetaModel getMetaModel() {
		return scopedUnchecked(super::getMetaModel);
	}

	@Override
	public ManipulationResponse applyManipulation(ManipulationRequest request) throws ModelAccessException {
		return scoped(() -> super.applyManipulation(request));
	}

	@Override
	public EntityQueryResult queryEntities(EntityQuery request) throws ModelAccessException {
		return scoped(() -> super.queryEntities(request));
	}

	@Override
	public PropertyQueryResult queryProperty(PropertyQuery request) throws ModelAccessException {
		return scoped(() -> super.queryProperty(request));
	}

	@Override
	public SelectQueryResult query(SelectQuery request) throws ModelAccessException {
		return scoped(() -> super.query(request));
	}

	@Override
	public ReferencesResponse getReferences(ReferencesRequest request) throws ModelAccessException {
		return scoped(() -> super.getReferences(request));
	}

	@Override
	public Set<String> getPartitions() throws ModelAccessException {
		return scoped(super::getPartitions);
	}

	@Override
	public Object processCustomRequest(ServiceRequestContext context, CustomPersistenceRequest request) {
		return scopedUnchecked(() -> super.processCustomRequest(context, request));
	}

	private <T> T scoped(AccessOperation<T> operation) throws ModelAccessException {
		if (userSession == null)
			return operation.get();

		AttributeContexts.push(identityContext());
		try {
			return operation.get();
		} finally {
			AttributeContexts.pop();
		}
	}

	private <T> T scopedUnchecked(UncheckedAccessOperation<T> operation) {
		if (userSession == null)
			return operation.get();

		AttributeContexts.push(identityContext());
		try {
			return operation.get();
		} finally {
			AttributeContexts.pop();
		}
	}

	private com.braintribe.common.attribute.AttributeContext identityContext() {
		AttributeContextBuilder builder = AttributeContexts.derivePeek();
		builder.set(UserSessionAspect.class, userSession);
		builder.set(IsAuthorizedAspect.class, true);
		builder.set(RequestorSessionIdAspect.class, userSession.getSessionId());
		String userName = userSession.getUser().getName();
		builder.set(RequestorUserNameAspect.class, userName);
		builder.set(UserInfoAttribute.class, UserInfo.of(userName, userSession.getEffectiveRoles()));
		builder.set(LenientAuthenticationFailure.class, null);
		return builder.build();
	}

	@FunctionalInterface
	private interface AccessOperation<T> {
		T get() throws ModelAccessException;
	}

	@FunctionalInterface
	private interface UncheckedAccessOperation<T> {
		T get();
	}
}
