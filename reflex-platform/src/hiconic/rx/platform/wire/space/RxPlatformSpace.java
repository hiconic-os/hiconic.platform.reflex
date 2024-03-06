package hiconic.rx.platform.wire.space;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.braintribe.codec.marshaller.common.BasicConfigurableMarshallerRegistry;
import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.platform.loading.RxModuleLoader;
import hiconic.rx.platform.service.RxServiceDomain;
import hiconic.rx.platform.service.RxServiceDomainConfigurations;
import hiconic.rx.platform.service.RxServiceDomains;
import hiconic.rx.platform.service.ServiceDomainDispatcher;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;

@Managed
public class RxPlatformSpace implements RxPlatformContract, RxProcessLaunchContract {

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
		for (RxModuleContract moduleContract: moduleContracts) {
			moduleContract.configureMainServiceDomain(mainDomain);
			moduleContract.configureServiceDomains(serviceDomainConfigurations);
			moduleContract.registerCrossDomainInterceptors(rootServiceProcessor());
			moduleContract.registerFallbackProcessors(fallbackProcessor());
		}
		
		// run all collected model meta data configurers
		// TODO: parallelize metadata editing per servicedomain as models are isolated
		RxServiceDomains serviceDomains = serviceDomains();
		for (RxServiceDomain serviceDomain: serviceDomains.list()) {
			BasicModelMetaDataEditor editor = new BasicModelMetaDataEditor(serviceDomain.getConfigurationModelBuilder().get());

			for (Consumer<ModelMetaDataEditor> configurer : serviceDomain.getModelConfigurers()) {
				configurer.accept(editor);
			}
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
		return bean;
	}
	
	@Override
	@Managed
	public RxServiceDomains serviceDomains() {
		RxServiceDomains bean = new RxServiceDomains();
		bean.setParentWireContext(wireContext);
		bean.setFallbackProcessor(fallbackProcessor());
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
	
	@Managed
	@Override
	public BasicConfigurableMarshallerRegistry marshallers() {
		BasicConfigurableMarshallerRegistry bean = new BasicConfigurableMarshallerRegistry();
		bean.registerMarshaller("application/json", jsonMarshaller());
		bean.registerMarshaller("text/yaml", yamlMarshaller());
		bean.registerMarshaller("application/yaml", yamlMarshaller());
		return bean;
	}
	
	@Managed
	private JsonStreamMarshaller jsonMarshaller() {
		return new JsonStreamMarshaller();
	}
	
	@Managed
	private YamlMarshaller yamlMarshaller() {
		return new YamlMarshaller();
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
	
	@Override
	@Managed
	public ConfigurableServiceRequestEvaluator evaluator() {
		ConfigurableServiceRequestEvaluator bean = new ConfigurableServiceRequestEvaluator();
		bean.setExecutorService(executorService());
		bean.setServiceProcessor(rootServiceProcessor());
		return bean;
	}

	@Override
	@Managed
	public ExecutorService executorService() {
		return Executors.newCachedThreadPool();
	}

	@Managed
	public ConfigurableDispatchingServiceProcessor rootServiceProcessor() {
		ConfigurableDispatchingServiceProcessor bean = new ConfigurableDispatchingServiceProcessor();

		bean.register(ServiceRequest.T, serviceDomainDispatcher());
		bean.registerInterceptor("domain-validation").register(serviceDomainDispatcher());
		
		return bean;
	}
	
	@Managed
	private ServiceDomainDispatcher serviceDomainDispatcher() {
		ServiceDomainDispatcher bean = new ServiceDomainDispatcher();
		bean.setServiceDomains(serviceDomains());
		return bean;
	}
	
	@Managed
	private ConfigurableDispatchingServiceProcessor fallbackProcessor() {
		ConfigurableDispatchingServiceProcessor bean = new ConfigurableDispatchingServiceProcessor();
		return bean;
	}
}
