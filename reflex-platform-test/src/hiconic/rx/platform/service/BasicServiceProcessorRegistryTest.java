package hiconic.rx.platform.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.module.api.service.ServiceProcessorRegistration;

public class BasicServiceProcessorRegistryTest {

	@Test
	public void keepsProcessorAndReflectionLazyAndStable() {
		BasicServiceProcessorRegistry registry = new BasicServiceProcessorRegistry();
		AtomicInteger processorCreations = new AtomicInteger();
		AtomicInteger reflectionCreations = new AtomicInteger();

		registry.register("remote", () -> {
			processorCreations.incrementAndGet();
			return (ServiceProcessor<ServiceRequest, Object>) (context, request) -> "ok";
		}, () -> {
			reflectionCreations.incrementAndGet();
			return InstanceId.T.create();
		});

		ServiceProcessorRegistration registration = registry.require("remote");
		assertEquals(0, processorCreations.get());
		assertEquals(0, reflectionCreations.get());
		assertSame(registration.processorSupplier().get(), registration.processorSupplier().get());
		assertSame(registration.reflectionSupplier().get(), registration.reflectionSupplier().get());
		assertEquals(1, processorCreations.get());
		assertEquals(1, reflectionCreations.get());
		assertEquals(1, registry.list().size());
	}

	@Test
	public void supportsRegistrationWithoutReflection() {
		BasicServiceProcessorRegistry registry = new BasicServiceProcessorRegistry();
		registry.register("plain", () -> (context, request) -> null);

		ServiceProcessorRegistration registration = registry.byKey("plain");
		assertNotNull(registration);
		assertNull(registration.reflectionSupplier());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsDuplicateKeys() {
		BasicServiceProcessorRegistry registry = new BasicServiceProcessorRegistry();
		registry.register("duplicate", () -> (context, request) -> null);
		registry.register("duplicate", () -> (context, request) -> null);
	}
}
