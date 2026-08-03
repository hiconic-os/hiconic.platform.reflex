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
import com.braintribe.gm.model.persistence.reflection.api.GetModelAndWorkbenchEnvironment;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.PersistenceServiceDomain;
import hiconic.rx.access.module.api.AccessServiceDomain;
import hiconic.rx.explorer.model.configuration.ExplorerConfiguration;
import hiconic.rx.explorer.processing.ExplorerServiceDomain;
import hiconic.rx.explorer.processing.WorkbenchReflectionProcessor;
import hiconic.rx.explorer.processing.bapi.AvailableAccessesProcessor;
import hiconic.rx.explorer.processing.bapi.CurrentUserInformationProcessor;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;
import hiconic.rx.webapi.model.meta.RequestMapping;
import hiconic.rx.module.api.service.ModelConfigurations;
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
	@Import private SystemToolsSpace systemTools;
	@Import private WebappsSpace webapps;
	
	// @formatter:on

	@Override
	public void configureModels(ModelConfigurations configurations) {
		cortex.configureCortexAccessModels();
	}

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration accessesSd = configurations.byId(AccessServiceDomain.accesses);
		accessesSd.setDisplayName("Accesses");
		accessesSd.bindRequest(AvailableAccessesRequest.T, this::availableAccessesProcessor);
		accessesSd.configureModel(editor -> editor.onEntityType(AvailableAccessesRequest.T)
				.addMetaData(availableAccessesMapping()));

		ServiceDomainConfiguration explorerSd = configurations.byId(ExplorerServiceDomain.explorer);
		explorerSd.setDisplayName("Explorer");

		explorerSd.bindRequest(CurrentUserInformationRequest.T, this::currentUserInformationProcessor);
		explorerSd.bindRequest(PlatformReflectionRequest.T, platformReflection::platformReflectionProcessor);

		ServiceDomainConfiguration persistenceSd = configurations.byId(PersistenceServiceDomain.persistence);
		persistenceSd.bindRequest(GetModelAndWorkbenchEnvironment.T, this::workbenchReflectionProcessor);
		persistenceSd.configureModel(editor -> editor.onEntityType(GetModelAndWorkbenchEnvironment.T)
				.addMetaData(modelEnvironmentMapping()));

		cortex.registerCortexAccess();
	}

	private RequestMapping availableAccessesMapping() {
		RequestMapping mapping = RequestMapping.T.create();
		mapping.setPath("available");
		mapping.setMethod(HttpRequestMethod.GET);
		mapping.setResponseMimeType("application/json");
		return mapping;
	}

	private RequestMapping modelEnvironmentMapping() {
		RequestMapping mapping = RequestMapping.T.create();
		// Preserve the generic endpoint URL used by the existing Explorer while
		// supplying the recurrence needed for Folder -> Folder -> Icon graphs.
		mapping.setPath(GetModelAndWorkbenchEnvironment.T.getTypeSignature());
		mapping.setMethod(HttpRequestMethod.GET);
		mapping.setResponseMimeType("application/json");
		mapping.setDepth("reachable");
		mapping.setEntityRecurrenceDepth(1);
		return mapping;
	}

	@Managed
	private WorkbenchReflectionProcessor workbenchReflectionProcessor() {
		WorkbenchReflectionProcessor bean = new WorkbenchReflectionProcessor();
		bean.setAccesses(access.accessDomains());
		bean.setSessionFactory(access.systemSessionFactory());
		bean.setConfiguration(platform.configuration().readConfig(ExplorerConfiguration.T).get());
		return bean;
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
	}

	@Override
	public void onApplicationReady() {
		systemTools.startTasks();
	}

}
