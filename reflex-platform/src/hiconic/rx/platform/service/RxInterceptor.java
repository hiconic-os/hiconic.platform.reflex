package hiconic.rx.platform.service;

import java.util.function.Predicate;

import com.braintribe.model.processing.service.api.ServiceInterceptorProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public class RxInterceptor {
	private ServiceInterceptorProcessor processor;
	private Predicate<ServiceRequest> predicate;
	
	public RxInterceptor(ServiceInterceptorProcessor processor) {
		this(processor, r -> true);
	}
	
	public RxInterceptor(ServiceInterceptorProcessor processor, Predicate<ServiceRequest> predicate) {
		super();
		this.processor = processor;
		this.predicate = predicate;
	}
	
	public <I extends ServiceInterceptorProcessor> I processor() {
		return (I)processor;
	}
	
	public Predicate<ServiceRequest> predicate() {
		return predicate;
	}
	
}
