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
package hiconic.rx.platform.models;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverBuilder;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.cmd.context.aspects.RoleAspect;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.usersession.UserSession;

public class RxCmdResolverManager {
	record CmdResolverKey(Set<String> roles, ModelOracle oracle) {}
	
	private Map<CmdResolverKey, CmdResolver> cache = new ConcurrentHashMap<>();

	public CmdResolver cmdResolver(AttributeContext attributeContext, ModelOracle modelOracle) {
		
		// get caching relevant aspect of current attribute context which are the user roles
		UserSession userSession = attributeContext.findOrNull(UserSessionAspect.class);
		Set<String> effectiveRoles = userSession != null? userSession.getEffectiveRoles(): Collections.emptySet();
		
		// access cache with the determined roles
		return cache.computeIfAbsent(new CmdResolverKey(effectiveRoles, modelOracle), k -> {
			
			CmdResolverBuilder builder = CmdResolverImpl.create(modelOracle);
			
			builder.addDynamicAspectProvider(RoleAspect.class, () -> k.roles());
			builder.setSessionProvider(() -> k);
			
			// TODO: please Peter add what is required in terms of Experts (e.g. Hibernate Dialect stuff)
			// builder.addExpert(null, null)
			
			return builder.done();
		});
		
	}
}
