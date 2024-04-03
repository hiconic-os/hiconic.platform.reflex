package hiconic.rx.platform.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

	private Supplier<CmdResolver> cmdResolverSupplier;
	private String domainId;
	private Evaluator<ServiceRequest> evaluator;
	private ConfigurationModelBuilder configurationModelBuilder;
	private ConfigurableDispatchingServiceProcessor dispatcher;
	private List<Consumer<ModelMetaDataEditor>> modelConfigurers = Collections.synchronizedList(new ArrayList<>());
	private Set<String> models = ConcurrentHashMap.newKeySet();
	private List<Supplier<? extends ServiceRequest>> defaultRequestSuppliers = Collections.synchronizedList(new ArrayList<>());
	
	@Required
	public void setDispatcher(ConfigurableDispatchingServiceProcessor dispatcher) {
		this.dispatcher = dispatcher;
	}
	
	@Required
	public void setConfigurationModelBuilder(ConfigurationModelBuilder configurationModelBuilder) {
		this.configurationModelBuilder = configurationModelBuilder;
	}
	
	@Required
	public void setCmdResolver(Supplier<CmdResolver> cmdResolver) {
		this.cmdResolverSupplier = cmdResolver;
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
		return cmdResolverSupplier.get();
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
		addModelByName(modelArtifactReflection.name());
		return this;
	}
	
	@Override
	public ServiceDomainConfiguration addModel(GmMetaModel gmModel) {
		if (models.add(gmModel.getName()))
			configurationModelBuilder.addDependency(gmModel);
		
		return this;
	}
	
	@Override
	public ServiceDomainConfiguration addModel(Model model) {
		addModelByName(model.name());
		
		return this;
	}
	
	@Override
	public ServiceDomainConfiguration addModelByName(String modelName) {
		if (models.add(modelName))
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
		addModel(requestType.getModel());
		dispatcher.register(requestType, serviceProcessor);
	}
	
	@Override
	public void configureModel(Consumer<ModelMetaDataEditor> configurer) {
		modelConfigurers.add(configurer);
	}
	
	@Override
	public void addDefaultRequestSupplier(Supplier<? extends ServiceRequest> defaultRequestSupplier) {
		defaultRequestSuppliers.add(defaultRequestSupplier);
	}
	
	public List<Consumer<ModelMetaDataEditor>> getModelConfigurers() {
		return modelConfigurers;
	}
	
	public ConfigurableDispatchingServiceProcessor getDispatcher() {
		return dispatcher;
	}
	
	@Override
	public ServiceRequest defaultRequest() {
		for (Supplier<? extends ServiceRequest> supplier: defaultRequestSuppliers) {
			ServiceRequest request = supplier.get();
			if (request != null)
				return request;
		}
		return null;
	}
}
