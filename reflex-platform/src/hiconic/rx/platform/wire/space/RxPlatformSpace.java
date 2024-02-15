package hiconic.rx.platform.wire.space;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.braintribe.codec.marshaller.common.BasicConfigurableMarshallerRegistry;
import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.configuration.ConfigurationModels;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContext;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.platform.reflex._ReflexPlatform_;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxProcessLaunchContract;
import hiconic.rx.platform.RxModuleLoader;
import hiconic.rx.platform.wire.contract.RxPlatformConfigContract;

@Managed
public class RxPlatformSpace implements RxPlatformContract, RxProcessLaunchContract {

	@Import
	private RxPlatformConfigContract config;
	
	@Import
	private WireContext<?> wireContext;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		// load service processing
		evaluator();
		
		// notify all modules about application being ready for action
		for (RxModuleContract moduleContract: moduleLoader().getModuleContracts()) {
			moduleContract.onApplicationReady();
		}
	}
	
	@Managed
	private RxModuleLoader moduleLoader() {
		RxModuleLoader bean = new RxModuleLoader();
		bean.setParentContext(wireContext);
		return bean;
	}
	
	@Managed
	@Override
	public CmdResolver mdResolver() {
		CmdResolver bean = CmdResolverImpl.create(modelOracle()).done();
		return bean;
	}
	
	@Managed
	@Override
	public ModelOracle modelOracle() {
		ModelOracle bean = new BasicModelOracle(configurationModel());
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
	
	@Managed
	public GmMetaModel configurationModel() {
		ConfigurationModelBuilder configurationModelBuilder = ConfigurationModels.create(_ReflexPlatform_.groupId, "configured-reflex-api-model");
		
		for (RxModuleContract moduleContract: moduleLoader().getModuleContracts()) {
			moduleContract.addApiModels(configurationModelBuilder);
		}
		
		GmMetaModel bean =  configurationModelBuilder.get();
		bean.setVersion(_ReflexPlatform_.version);
		
		BasicModelMetaDataEditor editor = new BasicModelMetaDataEditor(bean);
		
		for (RxModuleContract moduleContract: moduleLoader().getModuleContracts()) {
			moduleContract.configureApiModel(editor);
		}
		
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
	public ConfigurableServiceRequestEvaluator evaluator() {
		ConfigurableServiceRequestEvaluator bean = new ConfigurableServiceRequestEvaluator();
		bean.setExecutorService(executorService());
		bean.setServiceProcessor(selectingServiceProcessor());
		return bean;
	}

	@Managed
	public ExecutorService executorService() {
		return Executors.newCachedThreadPool();
	}

	@Managed
	public ConfigurableDispatchingServiceProcessor selectingServiceProcessor() {
		ConfigurableDispatchingServiceProcessor bean = new ConfigurableDispatchingServiceProcessor();

		for (RxModuleContract moduleContract: moduleLoader().getModuleContracts()) {
			moduleContract.registerProcessors(bean);
		}

		return bean;
	}
}
