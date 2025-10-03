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
package hiconic.rx.explorer.wire.space;

import com.braintribe.model.bapi.AvailableAccessesRequest;
import com.braintribe.model.bapi.CurrentUserInformationRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.explorer.processing.bapi.AvailableAccessesProcessor;
import hiconic.rx.explorer.processing.bapi.CurrentUserInformationProcessor;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.reflection.model.api.PlatformReflectionRequest;

/**
 * Module that brings support for tribefire-explorer
 */
@Managed
public class ExplorerRxModuleSpace implements RxModuleContract {

	// @formatter:off
	@Import private RxPlatformContract platform;

	@Import private AccessContract access;

	@Import private ChecksSpace checks;
	@Import private CortexSpace cortex;
	@Import private PlatformReflectionSpace platformReflection;
	@Import private WebappsSpace webapps;
	
	// @formatter:on

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration explorerSd = configurations.byId("explorer");

		explorerSd.bindRequest(AvailableAccessesRequest.T, this::availableAccessesProcessor);
		explorerSd.bindRequest(CurrentUserInformationRequest.T, this::currentUserInformationProcessor);
		explorerSd.bindRequest(PlatformReflectionRequest.T, platformReflection::platformReflectionProcessor);
	}

	@Managed
	private AvailableAccessesProcessor availableAccessesProcessor() {
		AvailableAccessesProcessor bean = new AvailableAccessesProcessor();
		bean.setAccessDomains(access.accessDomains());
		return bean;
	}

	@Managed
	private CurrentUserInformationProcessor currentUserInformationProcessor() {
		CurrentUserInformationProcessor bean = new CurrentUserInformationProcessor();
		return bean;
	}

	@Override
	public void onDeploy() {
		webapps.registerWebapps();
		checks.registerChecks();
		cortex.registerCortexAccess();
	}
}
