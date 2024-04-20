package hiconic.rx.module.api.service;

import java.util.function.Predicate;
import java.util.function.Supplier;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ServiceInterceptorProcessor;
import com.braintribe.model.service.api.ServiceRequest;

public interface InterceptorBuilder {
	InterceptorBuilder before(String identification);
	InterceptorBuilder after(String identification);
	InterceptorBuilder predicate(Predicate<ServiceRequest> predicate);
	InterceptorBuilder forType(EntityType<? extends ServiceRequest> requestType);
	void bind(Supplier<ServiceInterceptorProcessor> interceptorSupplier);
}
