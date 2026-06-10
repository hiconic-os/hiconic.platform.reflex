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
package hiconic.rx.platform.processing.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.model.processing.service.api.ReasonedServicePreProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.auth.RoleAuthorization;

public class RoleAuthorizationInterceptor implements ReasonedServicePreProcessor<ServiceRequest> {
	private RoleAuthorization roleAuthorization;
	private Set<String> roles = Collections.emptySet();
	private String message = "Insufficient privileges";

	@Override
	public Maybe<? extends ServiceRequest> processReasoned(ServiceRequestContext requestContext, ServiceRequest request) {
		if (!roleAuthorization.securityActive() || roleAuthorization.hasAnyRole(roles)) {
			return Maybe.complete(request);
		}

		return Reasons.build(Forbidden.T).text(message).toMaybe();
	}

	@Configurable
	@Required
	public void setRoleAuthorization(RoleAuthorization roleAuthorization) {
		this.roleAuthorization = Objects.requireNonNull(roleAuthorization, "roleAuthorization");
	}

	@Configurable
	public void setRoles(Collection<String> roles) {
		this.roles = roles == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(roles));
	}

	@Configurable
	public void setMessage(String message) {
		this.message = message;
	}
}
