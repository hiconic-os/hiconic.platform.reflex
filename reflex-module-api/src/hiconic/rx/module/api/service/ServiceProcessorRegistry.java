package hiconic.rx.module.api.service;

import java.util.List;
import java.util.function.Supplier;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.processing.service.api.ServiceProcessor;

/** Platform-wide registry of named, lazily instantiated {@link ServiceProcessor} components. */
public interface ServiceProcessorRegistry {

	void register(String key, Supplier<? extends ServiceProcessor<?, ?>> processorSupplier);

	void register(String key, Supplier<? extends ServiceProcessor<?, ?>> processorSupplier,
			Supplier<? extends GenericEntity> reflectionSupplier);

	default void register(ServiceProcessorSymbol symbol, Supplier<? extends ServiceProcessor<?, ?>> processorSupplier) {
		register(symbol.name(), processorSupplier);
	}

	default void register(ServiceProcessorSymbol symbol, Supplier<? extends ServiceProcessor<?, ?>> processorSupplier,
			Supplier<? extends GenericEntity> reflectionSupplier) {
		register(symbol.name(), processorSupplier, reflectionSupplier);
	}

	ServiceProcessorRegistration byKey(String key);

	default ServiceProcessorRegistration bySymbol(ServiceProcessorSymbol symbol) {
		return byKey(symbol.name());
	}

	default ServiceProcessorRegistration require(String key) {
		ServiceProcessorRegistration registration = byKey(key);
		if (registration == null)
			throw new IllegalArgumentException("No ServiceProcessor registered for key: " + key);
		return registration;
	}

	default ServiceProcessorRegistration require(ServiceProcessorSymbol symbol) {
		return require(symbol.name());
	}

	List<? extends ServiceProcessorRegistration> list();
}
