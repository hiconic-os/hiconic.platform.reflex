package hiconic.rx.platform.service;

import java.util.List;
import java.util.Objects;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.processing.meta.cmd.builders.EntityMdResolver;
import com.braintribe.model.processing.service.api.InterceptingServiceProcessorBuilder;
import com.braintribe.model.processing.service.api.ServiceInterceptionChainBuilder;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.common.ServiceProcessingChain;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.model.service.processing.md.InterceptWith;
import hiconic.rx.model.service.processing.md.InterceptionType;
import hiconic.rx.model.service.processing.md.ProcessWith;
import hiconic.rx.module.api.service.ServiceDomain;

/** 
 * @author Dirk Scheffler
 */
public class RxRequestDispatcher implements ServiceProcessor<ServiceRequest, Object> {

	private ServiceDomain serviceDomain;
	private ServiceProcessor<ServiceRequest,Object> fallbackProcessor;
	
	@Required
	@Configurable
	public void setServiceDomain(ServiceDomain serviceDomain) {
		this.serviceDomain = serviceDomain;
	}
	
	@Required
	@Configurable
	public void setFallbackProcessor(ServiceProcessor<ServiceRequest, Object> fallbackProcessor) {
		this.fallbackProcessor = fallbackProcessor;
	}
	
	@Override
	public Object process(ServiceRequestContext requestContext, ServiceRequest request) {
		ServiceProcessor<ServiceRequest,Object> enrichedProcessor = buildEnrichedProcessor(request);
		return enrichedProcessor.process(requestContext, request);
	}

	private ServiceProcessor<ServiceRequest, Object> resolveProcessor(EntityMdResolver mdResolver, ServiceRequest request) {
		ProcessWith processWith = mdResolver.meta(ProcessWith.T).exclusive();
		
		if (processWith == null)
			return fallbackProcessor;
		
		ServiceProcessor<ServiceRequest, Object> processor = processWith.getAssociate();
		
		return Objects.requireNonNull(processor, "ProcessWith misses assigned processor: " + processWith);
	}
	
	private ServiceProcessor<ServiceRequest, Object> buildEnrichedProcessor(ServiceRequest request) {
		EntityMdResolver mdResolver = serviceDomain.cmdResolver().getMetaData().entityType(request.entityType());
		
		ServiceProcessor<ServiceRequest, Object> processor = resolveProcessor(mdResolver, request);
		
		final InterceptingServiceProcessorBuilder interceptionBuilder;
		
		if (processor == null) {
			interceptionBuilder = ServiceProcessingChain.create();
		}
		else {
			interceptionBuilder = ServiceProcessingChain.create(processor);
		}
		
		if (request.interceptable())
			enrichWithInterceptors(request, mdResolver, interceptionBuilder);

		return interceptionBuilder.build();
	}
	
	
	private void enrichWithInterceptors(ServiceRequest request, EntityMdResolver mdResolver, ServiceInterceptionChainBuilder interceptionBuilder) {
		List<InterceptWith> interceptWiths = mdResolver.meta(InterceptWith.T).list();

		 
		for (InterceptWith interceptWith: interceptWiths) {
			RxInterceptor interceptor = Objects.requireNonNull(interceptWith.getAssociate(), "Corrupted InterceptWith");
			
			if (interceptor.predicate().test(request)) {
				InterceptionType interceptionType = interceptWith.interceptionType();
				switch (interceptionType) {
					case preProcess: 
						interceptionBuilder.preProcessWith(interceptor.processor());
						break;
					case aroundProcess: 
						interceptionBuilder.aroundProcessWith(interceptor.processor());
						break;
					case postProcess: 
						interceptionBuilder.postProcessWith(interceptor.processor());
						break;
					default:
						throw new UnsupportedOperationException("Unsupported InterceptorKind: " + interceptionType);
				}
			}
		}
	}
}