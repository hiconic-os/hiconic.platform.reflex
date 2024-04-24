package hiconic.rx.platform.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.common.eval.ConfigurableServiceRequestEvaluator;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.DelegatingModelConfiguration;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ServiceDomain;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.platform.models.RxConfiguredModel;

public class RxServiceDomain implements ServiceDomain, ServiceDomainConfiguration, DelegatingModelConfiguration {

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
	public ModelConfiguration modelConfiguration() {
		return modelConfiguration;
	}
	
	@Override
	public String domainId() {
		return domainId;
	}

	@Override
	public CmdResolver systemCmdResolver() {
		return modelConfiguration.systemCmdResolver();
	}
	
	@Override
	public CmdResolver contextCmdResolver() {
		return modelConfiguration.contextCmdResolver();
	}
	
	@Override
	public CmdResolver cmdResolver(AttributeContext attributeContext) {
		return modelConfiguration.cmdResolver(attributeContext);
	}

	@Override
	public ModelOracle modelOracle() {
		return systemCmdResolver().getModelOracle();
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
	public void addDefaultRequestSupplier(Supplier<? extends ServiceRequest> serviceRequestSupplier) {
		this.defaultRequestSuppliers.add(serviceRequestSupplier);
	}
}	
