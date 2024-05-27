package hiconic.rx.platform.wire.space;

import java.io.File;
import java.util.List;

import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.platform.loading.RxModuleLoader;
import hiconic.rx.platform.models.RxModelConfigurations;
import hiconic.rx.platform.service.RxServiceDomain;
import hiconic.rx.platform.service.RxServiceDomainConfigurations;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;

@Managed
public class RxPlatformSpace extends CoreServicesSpace implements RxPlatformContract, RxProcessLaunchContract {

	@Import
	private RxPlatformConfigContract config;
	
	@Import
	private WireContext<?> wireContext;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		configureModules();
	}
	
	private void configureModules() {
		List<RxModuleContract> moduleContracts = moduleLoader().listModuleContracts();

		RxServiceDomainConfigurations serviceDomainConfigurations = serviceDomainConfigurations();
		RxServiceDomain mainDomain = serviceDomainConfigurations.main();
		
		// run service domain configuration of all modules
		// TODO: parallelize
		RxModelConfigurations modelConfigurations = modelConfigurations();
		
		for (RxModuleContract moduleContract: moduleContracts) {
			
			moduleContract.configureMainPersistenceModel(modelConfigurations.mainPersistenceModel());
			moduleContract.configureModels(modelConfigurations);
			
			moduleContract.configureMainServiceDomain(mainDomain);
			moduleContract.configureServiceDomains(serviceDomainConfigurations);
			
			moduleContract.registerCrossDomainInterceptors(rootServiceProcessor());
			moduleContract.registerFallbackProcessors(fallbackProcessor());
			
		}
		
		// run all collected model meta data configurers
		// TODO: parallelize metadata editing per model as models are isolated
		// TODO: do that based on configuration because there is a tradeoff between bootstrap time and bootstrap completeness
		for (ConfiguredModel configuredModel: configuredModels().list()) {
			// TODO: trigger eager configuration
			configuredModel.systemCmdResolver();
		}

		// notify all modules about application being ready for action
		for (RxModuleContract moduleContract: moduleContracts) {
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
		return bean;
	}
	
	@Override
	public <C extends GenericEntity> Maybe<C> readConfig(EntityType<C> configType) {
		return modeledConfiguration().configReasoned(configType);
	}
	
	@Managed
	private ModeledYamlConfiguration modeledConfiguration() {
		ModeledYamlConfiguration bean = new ModeledYamlConfiguration();
		bean.setConfigFolder(new File(config.appDir(), "conf"));
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
	public String nodeId() {
		return "main";
	}

}
