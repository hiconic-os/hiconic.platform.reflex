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

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.AttributeContextBuilder;
import com.braintribe.common.attribute.common.UserInfo;
import com.braintribe.common.attribute.common.UserInfoAttribute;
import com.braintribe.model.processing.securityservice.api.attributes.LenientAuthenticationFailure;
import com.braintribe.model.processing.service.api.aspect.IsAuthorizedAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorSessionIdAspect;
import com.braintribe.model.processing.service.api.aspect.RequestorUserNameAspect;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

/**
 * Derives from the current invocation context and overlays only the stable identity of a persistence session.
 */
final class SessionIdentityContext {

	private SessionIdentityContext() {
	}

	static AttributeContext derive(UserSession userSession) {
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
}
