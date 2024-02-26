package hiconic.rx.platform.service.wire.space;

import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.configuration.ConfigurationModels;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContext;

import hiconic.platform.reflex._ReflexPlatform_;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.service.RxServiceDomain;
import hiconic.rx.platform.service.wire.contract.RxServiceDomainConfigContract;
import hiconic.rx.platform.service.wire.contract.RxServiceDomainContract;

@Managed
public class RxServiceDomainSpace implements RxServiceDomainContract {

	@Import
	private RxServiceDomainConfigContract config;
	
	@Import
	private WireContext<?> wireContext;
	
	@Import
	private RxPlatformContract platform;
	
	@Managed
	@Override
	public CmdResolver cmdResolver() {
		CmdResolver bean = CmdResolverImpl.create(modelOracle()).done();
		return bean;
	}
	
	@Managed
	@Override
	public ModelOracle modelOracle() {
		ModelOracle bean = new BasicModelOracle(configurationModel());
		return bean;
	}
	
	@Managed
	private GmMetaModel configurationModel() {
		return configurationModelBuilder().get();
	}

	@Override
	@Managed
	public ConfigurationModelBuilder configurationModelBuilder() {
		ConfigurationModelBuilder bean = ConfigurationModels.create(_ReflexPlatform_.groupId, "configured-" + config.domainId() + "-api-model");
		return bean;
	}
	
	@Override
	@Managed
	public ConfigurableServiceRequestEvaluator evaluator() {
		ConfigurableServiceRequestEvaluator bean = new ConfigurableServiceRequestEvaluator();
		bean.setExecutorService(platform.executorService());
		bean.setServiceProcessor(selectingServiceProcessor());
		return bean;
	}

	@Override
	@Managed
	public ConfigurableDispatchingServiceProcessor selectingServiceProcessor() {
		ConfigurableDispatchingServiceProcessor bean = new ConfigurableDispatchingServiceProcessor();
		bean.register(ServiceRequest.T, config.fallbackProcessor());
		return bean;
	}
	
	@Override
	@Managed
	public RxServiceDomain serviceDomain() {
		RxServiceDomain bean = new RxServiceDomain();
		bean.setDomainId(config.domainId());
		bean.setEvaluator(evaluator());
		bean.setMdResolver(cmdResolver());
		bean.setConfigurationModelBuilder(configurationModelBuilder());
		bean.setDispatcher(selectingServiceProcessor());
		return bean;
	}
}
