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
package hiconic.rx.platform.wire.space;

import java.util.List;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.module.api.config.RxPlatformConfigurator;
import hiconic.rx.module.api.log.RxLogManager;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.state.RxApplicationState;
import hiconic.rx.module.api.state.RxApplicationStateManager;
import hiconic.rx.module.api.wire.RxAuthContract;
import hiconic.rx.module.api.wire.RxConfigurationContract;
import hiconic.rx.module.api.wire.RxExecutionContract;
import hiconic.rx.module.api.wire.RxMarshallingContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformResourcesContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.module.api.wire.RxServiceProcessingContract;
import hiconic.rx.module.api.wire.RxTransientDataContract;
import hiconic.rx.platform.conf.RxPlatformConfiguratorImpl;
import hiconic.rx.platform.loading.RxModuleLoader;
import hiconic.rx.platform.resource.RxResourcesStorages;
import hiconic.rx.platform.service.RxServiceDomain;
import hiconic.rx.platform.service.RxServiceDomainConfigurations;
import hiconic.rx.platform.wire.contract.ExtendedRxPlatformContract;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;

@Managed
public class RxPlatformSpace extends CoreServicesSpace implements ExtendedRxPlatformContract, RxProcessLaunchContract {

	// @formatter:off
	@Import private RxApplicationSpace application;
	@Import private RxAuthContract auth;
	@Import private RxConfigurationSpace configuration;
	@Import private RxExecutionSpace execution;
	@Import private RxMarshallingSpace marshalling;
	@Import private RxPlatformConfigContract config;
	@Import private RxPlatformResourcesContract platformResources;
	@Import private RxServiceProcessingSpace serviceProcessing;
	@Import private RxTransientDataContract transientData;
	@Import private WireContext<?> wireContext;
	// @formatter:on

	// @formatter:off
	@Override public RxApplicationSpace application() { return application; }	
	@Override public RxAuthContract auth() { return auth; }
	@Override public RxConfigurationContract configuration() { return configuration; }
	@Override public RxExecutionContract execution() { return execution; }
	@Override public RxMarshallingContract marshalling() { return marshalling; }
	@Override public RxPlatformResourcesContract platformResources() { return platformResources; }
	@Override public RxProcessLaunchContract processLaunch() { return this; }
	@Override public RxServiceProcessingContract serviceProcessing() { return serviceProcessing; }
	@Override public RxTransientDataContract transientData() { return transientData; }
	// @formatter:on

	// ######################################
	// ## . . . . . . Lifecycle . . . . . .##
	// ######################################

	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		application.stateManager().setState(RxApplicationState.starting);
		configureModules();
		application.stateManager().setState(RxApplicationState.started);
	}

	private void configureModules() {
		List<RxModuleContract> moduleContracts = moduleLoader().listModuleContracts();

		RxServiceDomainConfigurations serviceDomainConfigurations = serviceDomainConfigurations();
		RxServiceDomain mainDomain = serviceDomainConfigurations.main();

		// run service domain configuration of all modules
		var modelConfigurations = configuration.modelConfigurations();

		for (RxModuleContract moduleContract : moduleContracts) {
			moduleContract.configureMainPersistenceModel(modelConfigurations.mainPersistenceModel());
			moduleContract.configureModels(modelConfigurations);
		}

		for (RxModuleContract moduleContract : moduleContracts) {
			moduleContract.configureMainServiceDomain(mainDomain);
			moduleContract.configureServiceDomains(serviceDomainConfigurations);

			moduleContract.registerCrossDomainInterceptors(serviceProcessing.rootServiceProcessor());
			moduleContract.registerFallbackProcessors(serviceProcessing.fallbackProcessor());

			moduleContract.configurePlatform(platformConfigurator());
		}

		modelConfigurations.finalizeModelConfiguration();

		// notify all modules about application being ready for the deployment inside modules
		for (RxModuleContract moduleContract : moduleContracts) {
			moduleContract.onDeploy();
		}

		for (ConfiguredModel configuredModel : configuration.configuredModels().list()) {
			configuredModel.systemCmdResolver();
		}

		// notify all modules about application being ready for action
		for (RxModuleContract moduleContract : moduleContracts) {
			moduleContract.onApplicationReady();
		}
	}

	private RxServiceDomainConfigurations serviceDomainConfigurations() {
		RxServiceDomainConfigurations bean = new RxServiceDomainConfigurations();
		bean.setServiceDomains(serviceProcessing.serviceDomains());
		return bean;
	}

	@Managed
	private RxModuleLoader moduleLoader() {
		RxModuleLoader bean = new RxModuleLoader();
		bean.setExecutorService(execution.executorService());
		bean.setParentContext(wireContext);
		bean.setContractSpaceResolverConfigurator(config.contractSpaceResolverConfigurator());
		bean.setPropertyResolver(configuration.propertyResolver());
		return bean;
	}

	@Override
	public void onApplicationShutdown() {
		moduleLoader().onApplicationShutdown();
	}

	@Managed
	private RxPlatformConfigurator platformConfigurator() {
		RxPlatformConfiguratorImpl bean = new RxPlatformConfiguratorImpl();
		bean.workerManagerHolder = execution.workerManagerHolder();
		bean.workerAspectRegistry = execution.workerAspectRegistry();
		bean.marshallerRegistry = marshalling.marshallers();
		bean.resourceStorages = resourceStorages();

		return bean;
	}

	@Override
	@Managed
	public RxResourcesStorages resourceStorages() {
		RxResourcesStorages bean = new RxResourcesStorages();
		return bean;
	}

	// ######################################
	// ## . . RxProcessLaunchContract . . .##
	// ######################################

	@Override
	public String[] cliArguments() {
		return config.cliArguments();
	}

	@Override
	public String launchScriptName() {
		return config.launchScriptName();
	}

	// ######################################
	// ## . . Deprecated delegations . . . ##
	// ######################################

	// @formatter:off
	@Override @Deprecated public <C extends GenericEntity> Maybe<C> readConfig(EntityType<C> configType) { return configuration.readConfig(configType); }
	@Override @Deprecated public RxLogManager logManager() { return application.logManager(); }
	@Override @Deprecated public RxApplicationStateManager stateManager() { return application.stateManager(); }
	@Override @Deprecated public String applicationName() { return application.applicationName(); }
	@Override @Deprecated public String applicationId() { return application.applicationId(); }
	@Override @Deprecated public String nodeId() { return application.nodeId(); }
	@Override @Deprecated public InstanceId instanceId() { return application.instanceId(); }
	@Override @Deprecated public WorkerManager workerManager() { return execution.workerManager(); }
	// @formatter:on

}
