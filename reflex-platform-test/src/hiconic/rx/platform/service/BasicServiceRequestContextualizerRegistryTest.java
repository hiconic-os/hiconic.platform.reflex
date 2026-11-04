package hiconic.rx.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class BasicServiceRequestContextualizerRegistryTest {

	@Test
	public void nestsRegisteredContextsInRegistrationOrder() throws Exception {
		BasicServiceRequestContextualizerRegistry registry = new BasicServiceRequestContextualizerRegistry();
		List<String> events = new ArrayList<>();

		registry.register((request, execution) -> {
			events.add("first-enter");
			try {
				execution.execute();
			} finally {
				events.add("first-leave");
			}
		});
		registry.register((request, execution) -> {
			events.add("second-enter");
			try {
				execution.execute();
			} finally {
				events.add("second-leave");
			}
		});

		registry.contextualize(null, () -> events.add("request"));

		assertThat(events).containsExactly("first-enter", "second-enter", "request", "second-leave", "first-leave");
	}
}
