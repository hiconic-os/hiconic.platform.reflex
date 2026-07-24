package hiconic.rx.module.api.service;

import java.util.function.Supplier;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.processing.service.api.ServiceProcessor;

/** A named, lazily instantiated service processor and its optional modeled reflection. */
public interface ServiceProcessorRegistration {

	String key();

	/** The registry guarantees that this supplier resolves a single lazy processor instance. */
	Supplier<? extends ServiceProcessor<?, ?>> processorSupplier();

	/**
	 * Optional modeled description of the registered component. Returns {@code null} when no reflection was registered.
	 */
	Supplier<? extends GenericEntity> reflectionSupplier();
}
