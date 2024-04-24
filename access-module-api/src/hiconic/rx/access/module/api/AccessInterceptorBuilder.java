package hiconic.rx.access.module.api;

import java.util.function.Supplier;

import com.braintribe.model.processing.aop.api.aspect.AccessAspect;

public interface AccessInterceptorBuilder {
	AccessInterceptorBuilder before(String identification);
	AccessInterceptorBuilder after(String identification);
	void bind(Supplier<AccessAspect> aspectSupplier);
}