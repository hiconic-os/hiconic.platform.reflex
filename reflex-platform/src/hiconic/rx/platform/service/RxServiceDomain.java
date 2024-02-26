package hiconic.rx.platform.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.braintribe.cfg.Required;
import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.configured.ConfigurationModelBuilder;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.InterceptorRegistration;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.common.ConfigurableDispatchingServiceProcessor;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;

public class RxServiceDomain implements ServiceDomain, ServiceDomainConfiguration {

	private CmdResolver mdResolver;
	private String domainId;
	private Evaluator<ServiceRequest> evaluator;
	private ConfigurationModelBuilder configurationModelBuilder;
	private ConfigurableDispatchingServiceProcessor dispatcher;
	private List<Consumer<ModelMetaDataEditor>> modelConfigurers = Collections.synchronizedList(new ArrayList<>());
	
	@Required
	public void setDispatcher(ConfigurableDispatchingServiceProcessor dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	@Required
	public void setConfigurationModelBuilder(ConfigurationModelBuilder configurationModelBuilder) {
		this.configurationModelBuilder = configurationModelBuilder;
	}
	
	@Required
	public void setMdResolver(CmdResolver mdResolver) {
		this.mdResolver = mdResolver;
	}

	@Required
	public void setDomainId(String domainId) {
		this.domainId = domainId;
	}

	@Required
	public void setEvaluator(Evaluator<ServiceRequest> evaluator) {
		this.evaluator = evaluator;
	}
	
	public ConfigurationModelBuilder getConfigurationModelBuilder() {
		return configurationModelBuilder;
	}

	@Override
	public String domainId() {
		return domainId;
	}

	@Override
	public CmdResolver cmdResolver() {
		return mdResolver;
	}

	@Override
	public ModelOracle modelOracle() {
		return cmdResolver().getModelOracle();
	}

	@Override
	public Evaluator<ServiceRequest> evaluator() {
		return evaluator;
	}
	
	@Override
	public ServiceDomainConfiguration addModel(ArtifactReflection modelArtifactReflection) {
		configurationModelBuilder.addDependency(modelArtifactReflection);
		return this;
	}
	
	@Override
	public ServiceDomainConfiguration addModel(GmMetaModel gmModel) {
		configurationModelBuilder.addDependency(gmModel);
		return this;
	}
	
	@Override
	public ServiceDomainConfiguration addModel(Model model) {
		configurationModelBuilder.addDependency(model);
		return this;
	}
	
	@Override
	public ServiceDomainConfiguration addModelByName(String modelName) {
		configurationModelBuilder.addDependencyByName(modelName);
		return this;
	}
	
	@Override
	public InterceptorRegistration registerInterceptor(String identification) {
		return dispatcher.registerInterceptor(identification);
	}
	
	@Override
	public <R extends ServiceRequest> void register(EntityType<R> requestType,
			ServiceProcessor<? super R, ?> serviceProcessor) {
		dispatcher.register(requestType, serviceProcessor);
	}
	
	@Override
	public void configureModel(Consumer<ModelMetaDataEditor> configurer) {
		modelConfigurers.add(configurer);
	}
	
	public List<Consumer<ModelMetaDataEditor>> getModelConfigurers() {
		return modelConfigurers;
	}
	
	public ConfigurableDispatchingServiceProcessor getDispatcher() {
		return dispatcher;
	}
}
