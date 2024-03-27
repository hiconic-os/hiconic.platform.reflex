package hiconic.rx.module.api.service;

import java.util.function.Supplier;

import com.braintribe.model.service.api.ServiceRequest;

public interface ServiceDomainConfiguration extends ModelConfiguration {
	/**
	 * Adds a {@link Supplier} for a {@link ServiceRequest} that will be used when a service domain is asked to give a default request via {@link ServiceDomain#defaultRequest()}.
	 * 
	 * The supplier may return null if it has nothing to contribute in a given context. Then subsequent suppliers will be asked.
	 */
	void addDefaultRequestSupplier(Supplier<? extends ServiceRequest> serviceRequestSupplier);
}
