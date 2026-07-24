package hiconic.rx.platform.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.utils.lcd.Lazy;
import com.braintribe.utils.lcd.NullSafe;

import hiconic.rx.module.api.service.ServiceProcessorRegistration;
import hiconic.rx.module.api.service.ServiceProcessorRegistry;

/** Default thread-safe implementation of the platform's named processor registry. */
public class BasicServiceProcessorRegistry implements ServiceProcessorRegistry {

	private final Map<String, ServiceProcessorRegistration> registrations = new ConcurrentHashMap<>();

	@Override
	public void register(String key, Supplier<? extends ServiceProcessor<?, ?>> processorSupplier) {
		register(key, processorSupplier, null);
	}

	@Override
	public void register(String key, Supplier<? extends ServiceProcessor<?, ?>> processorSupplier,
			Supplier<? extends GenericEntity> reflectionSupplier) {
		NullSafe.nonNull(key, "ServiceProcessor key");
		NullSafe.nonNull(processorSupplier, "ServiceProcessor supplier");
		if (key.isBlank())
			throw new IllegalArgumentException("ServiceProcessor key must not be blank.");

		Registration registration = new Registration(key, processorSupplier, reflectionSupplier);
		if (registrations.putIfAbsent(key, registration) != null)
			throw new IllegalArgumentException("Duplicate ServiceProcessor registration for key: " + key);
	}

	@Override
	public ServiceProcessorRegistration byKey(String key) {
		return registrations.get(key);
	}

	@Override
	public List<? extends ServiceProcessorRegistration> list() {
		return registrations.values().stream().sorted((a, b) -> a.key().compareTo(b.key())).toList();
	}

	private static class Registration implements ServiceProcessorRegistration {
		private final String key;
		private final Lazy<ServiceProcessor<?, ?>> processor;
		private final Lazy<GenericEntity> reflection;

		private Registration(String key, Supplier<? extends ServiceProcessor<?, ?>> processorSupplier,
				Supplier<? extends GenericEntity> reflectionSupplier) {
			this.key = key;
			this.processor = new Lazy<>(processorSupplier::get);
			this.reflection = reflectionSupplier == null ? null : new Lazy<>(reflectionSupplier::get);
		}

		@Override
		public String key() {
			return key;
		}

		@Override
		public Supplier<? extends ServiceProcessor<?, ?>> processorSupplier() {
			return processor::get;
		}

		@Override
		public Supplier<? extends GenericEntity> reflectionSupplier() {
			return reflection == null ? null : reflection::get;
		}
	}
}
