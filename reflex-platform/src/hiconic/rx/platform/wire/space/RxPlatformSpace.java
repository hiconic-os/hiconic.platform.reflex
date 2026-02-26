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

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.worker.api.ConfigurableWorkerAspectRegistry;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.provider.Box;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.module.api.log.RxLogManager;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.state.RxApplicationState;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformConfigurator;
import hiconic.rx.module.api.wire.RxPlatformResourcesContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.platform.conf.RxPlatformConfiguratorImpl;
import hiconic.rx.platform.conf.RxPropertyResolver;
import hiconic.rx.platform.loading.RxModuleLoader;
import hiconic.rx.platform.loading.RxPropertiesLoader;
import hiconic.rx.platform.log.RxLogManagerImpl;
import hiconic.rx.platform.model.configuration.ReflexAppConfiguration;
import hiconic.rx.platform.models.RxModelConfigurations;
import hiconic.rx.platform.processing.lifez.DeadlockChecker;
import hiconic.rx.platform.processing.worker.BasicRxWorkerManager;
import hiconic.rx.platform.processing.worker.BasicWorkerAspectRegistry;
import hiconic.rx.platform.service.RxServiceDomain;
import hiconic.rx.platform.service.RxServiceDomainConfigurations;
import hiconic.rx.platform.state.RxApplicationStateManagerImpl;
import hiconic.rx.platform.wire.contract.ExtendedRxPlatformContract;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;

@Managed
public class RxPlatformSpace extends CoreServicesSpace implements ExtendedRxPlatformContract, RxProcessLaunchContract {

	@Import
	private RxPlatformConfigContract config;

	@Import
	private RxPlatformResourcesContract platformResources;

	@Import
	private WireContext<?> wireContext;

	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		stateManager().setState(RxApplicationState.starting);
		configureModules();
		stateManager().setState(RxApplicationState.started);
	}

	private void configureModules() {
		List<RxModuleContract> moduleContracts = moduleLoader().listModuleContracts();

		RxServiceDomainConfigurations serviceDomainConfigurations = serviceDomainConfigurations();
		RxServiceDomain mainDomain = serviceDomainConfigurations.main();

		// run service domain configuration of all modules
		RxModelConfigurations modelConfigurations = modelConfigurations();

		for (RxModuleContract moduleContract : moduleContracts) {
			moduleContract.configureMainPersistenceModel(modelConfigurations.mainPersistenceModel());
			moduleContract.configureModels(modelConfigurations);
		}

		for (RxModuleContract moduleContract : moduleContracts) {
			moduleContract.configureMainServiceDomain(mainDomain);
			moduleContract.configureServiceDomains(serviceDomainConfigurations);

			moduleContract.registerCrossDomainInterceptors(rootServiceProcessor());
			moduleContract.registerFallbackProcessors(fallbackProcessor());

			moduleContract.configurePlatform(platformConfigurator());
		}

		for (ConfiguredModel configuredModel : configuredModels().list()) {
			// TODO: trigger eager configuration
			configuredModel.systemCmdResolver();
		}

		// notify all modules about application being ready for the deployment inside modules
		for (RxModuleContract moduleContract : moduleContracts) {
			moduleContract.onDeploy();
		}

		// notify all modules about application being ready for action
		for (RxModuleContract moduleContract : moduleContracts) {
			moduleContract.onApplicationReady();
		}
	}

	private RxServiceDomainConfigurations serviceDomainConfigurations() {
		RxServiceDomainConfigurations bean = new RxServiceDomainConfigurations();
		bean.setServiceDomains(serviceDomains());
		return bean;
	}

	@Managed
	private RxModuleLoader moduleLoader() {
		RxModuleLoader bean = new RxModuleLoader();
		bean.setExecutorService(executorService());
		bean.setParentContext(wireContext);
		bean.setContractSpaceResolverConfigurator(config.contractSpaceResolverConfigurator());
		bean.setPropertyResolver(propertyResolver());
		return bean;
	}

	@Override
	public <C extends GenericEntity> Maybe<C> readConfig(EntityType<C> configType) {
		return modeledConfiguration().configReasoned(configType);
	}

	@Managed
	private ModeledYamlConfiguration modeledConfiguration() {
		ModeledYamlConfiguration bean = new ModeledYamlConfiguration();
		bean.setConfigFolder(platformResources.confPath().toFile());
		bean.setExternalReasonedPropertyLookup(propertyResolver()::resolveReasoned);
		return bean;
	}

	@Managed
	private RxPropertyResolver propertyResolver() {
		RxPropertyResolver bean = new RxPropertyResolver();

		Map<String, String> rawProperties = getOrTunnel(
				RxPropertiesLoader.loadFromFolder(platformResources.confPath().toFile(), "properties(-.*)?.yaml", yamlMarshaller()));
		bean.setRawProperties(rawProperties);
		return bean;
	}

	@Override
	public String[] cliArguments() {
		return config.cliArguments();
	}

	@Override
	public String launchScriptName() {
		return config.launchScriptName();
	}

	@Override
	public String applicationName() {
		return config.properties().applicationName();
	}

	@Override
	@Managed
	public RxLogManager logManager() {
		RxLogManagerImpl bean = new RxLogManagerImpl();
		return bean;
	}

	@Managed
	private RxPlatformConfigurator platformConfigurator() {
		RxPlatformConfiguratorImpl bean = new RxPlatformConfiguratorImpl();
		bean.workerManagerHolder = workerManagerHolder();
		bean.workerAspectRegistry = workerAspectRegistry();
		bean.marshallerRegistry = marshallers();
		bean.resourceStorages = resourceStorages();

		return bean;
	}

	@Override
	public WorkerManager workerManager() {
		return workerManagerHolder().value;
	}

	@Managed
	private Box<WorkerManager> workerManagerHolder() {
		Box<WorkerManager> bean = Box.of(defaultWorkerManager());

		return bean;
	}

	private BasicRxWorkerManager defaultWorkerManager() {
		BasicRxWorkerManager result = new BasicRxWorkerManager();
		result.setExecutorService(executorService());
		result.setApplicationId(applicationId());

		return result;
	}

	@Managed
	private ConfigurableWorkerAspectRegistry workerAspectRegistry() {
		BasicWorkerAspectRegistry bean = new BasicWorkerAspectRegistry();

		return bean;
	}

	@Override
	@Managed
	public RxApplicationStateManagerImpl stateManager() {
		RxApplicationStateManagerImpl bean = new RxApplicationStateManagerImpl();
		bean.addLivenessChecker(new DeadlockChecker());
		return bean;
	}

	@Override
	@Managed
	public String applicationId() {
		String bean = resolveApplicationId();
		return bean;
	}

	private String resolveApplicationId() {
		String result = appConfiguration().getApplicationId();
		if (result == null)
			result = appIdFromAppName();

		return result;
	}

	private String appIdFromAppName() {
		return applicationName().replace(" ", "-");
	}

	@Override
	@Managed
	public String nodeId() {
		String bean = resolveNodeId();
		return bean;
	}

	private String resolveNodeId() {
		String result = appConfiguration().getNodeId();
		if (result == null)
			result = UUID.randomUUID().toString();

		return result;
	}

	@Override
	@Managed
	public InstanceId instanceId() {
		InstanceId bean = InstanceId.T.create();
		bean.setApplicationId(applicationId());
		bean.setNodeId(nodeId());
		return bean;
	}

	private ReflexAppConfiguration appConfiguration() {
		return readConfig(ReflexAppConfiguration.T).get();
	}

}