package hiconic.rx.platform.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.InterceptorRegistration;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ConfiguredModelReference;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.platform.models.RxConfiguredModel;

public class RxServiceDomain implements ServiceDomain, ServiceDomainConfiguration {

	private String domainId;
	private ConfigurableServiceRequestEvaluator evaluator;

	private RxConfiguredModel modelConfiguration;
	private RxRequestDispatcher dispatcher;
	
	private List<Supplier<? extends ServiceRequest>> defaultRequestSuppliers = Collections.synchronizedList(new ArrayList<>());
	
	public RxServiceDomain(String domainId, RxConfiguredModel modelConfiguration, ExecutorService executorService, Evaluator<ServiceRequest> contextEvaluator, ServiceProcessor<ServiceRequest, Object> fallbackProcessor) {
		this.domainId = domainId;
		this.modelConfiguration = modelConfiguration;
		
		dispatcher = new RxRequestDispatcher();
		dispatcher.setServiceDomain(this);
		dispatcher.setFallbackProcessor(fallbackProcessor);
		
		evaluator = new ConfigurableServiceRequestEvaluator();
		evaluator.setExecutorService(executorService);
		evaluator.setServiceProcessor(dispatcher);
		evaluator.setContextEvaluator(contextEvaluator);
	}
	
	@Override
	public String modelName() {
		return modelConfiguration.modelName();
	}

	@Override
	public String domainId() {
		return domainId;
	}

	@Override
	public CmdResolver cmdResolver() {
		return modelConfiguration.cmdResolver();
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
	public ServiceRequest defaultRequest() {
		for (Supplier<? extends ServiceRequest> supplier: defaultRequestSuppliers) {
			ServiceRequest request = supplier.get();
			if (request != null)
				return request;
		}
		
		return null;
	}
	
	@Override
	public void addModel(ConfiguredModelReference modelReference) {
		modelConfiguration.addModel(modelReference);
	}
	
	@Override
	public void addModel(ArtifactReflection modelArtifactReflection) {
		modelConfiguration.addModel(modelArtifactReflection);
	}
	
	@Override
	public void addModel(GmMetaModel gmModel) {
		modelConfiguration.addModel(gmModel);
	}
	
	@Override
	public void addModel(Model model) {
		modelConfiguration.addModel(model);
	}
	
	@Override
	public void addModelByName(String modelName) {
		modelConfiguration.addModelByName(modelName);
	}

	@Override
	public void addDefaultRequestSupplier(Supplier<? extends ServiceRequest> serviceRequestSupplier) {
		this.defaultRequestSuppliers.add(serviceRequestSupplier);
	}
	
	@Override
	public InterceptorRegistration registerInterceptor(String identification) {
		return modelConfiguration.registerInterceptor(identification);
	}
	
	@Override
	public <R extends ServiceRequest> void register(EntityType<R> requestType,
			ServiceProcessor<? super R, ?> serviceProcessor) {
		modelConfiguration.register(requestType, serviceProcessor);
	}
	
	@Override
	public void configureModel(Consumer<ModelMetaDataEditor> configurer) {
		modelConfiguration.configureModel(configurer);
	}
}
