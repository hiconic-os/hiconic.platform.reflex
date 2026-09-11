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
package hiconic.rx.web.ddra.mapping;

import java.util.Set;

import com.braintribe.model.resourceapi.stream.GetResource;
import com.braintribe.model.securityservice.Logout;
import com.braintribe.model.securityservice.OpenUserSessionWithUserAndPassword;

import hiconic.rx.security.api.SecurityServiceDomain;
import hiconic.rx.web.ddra.endpoints.api.WebApiMappingRegistry;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;

/** Registers domain-free Web API paths supplied by the RX platform. */
public final class PlatformWebApiMappings {

	private static final Set<String> SECURITY_TAGS = Set.of("security");

	private PlatformWebApiMappings() {
	}

	public static void register(WebApiMappingRegistry mappings) {
		mappings.mapping("/download", HttpRequestMethod.GET, GetResource.T) //
				.responseProjection("resource") //
				.downloadResource(true) //
				.register();

		mappings.mapping("/authenticate", HttpRequestMethod.POST, OpenUserSessionWithUserAndPassword.T) //
				.serviceDomain(SecurityServiceDomain.security.name()) //
				.responseProjection("userSession.sessionId") //
				.tags(SECURITY_TAGS) //
				.register();

		mappings.mapping("/logout", HttpRequestMethod.POST, Logout.T) //
				.serviceDomain(SecurityServiceDomain.security.name()) //
				.tags(SECURITY_TAGS) //
				.register();
	}
}
