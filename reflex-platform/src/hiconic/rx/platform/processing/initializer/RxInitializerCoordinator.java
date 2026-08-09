// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.processing.initializer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.braintribe.gm.initializer.api.InitializerFingerprintResolver;
import com.braintribe.gm.initializer.api.InitializerRegistry;
import com.braintribe.gm.initializer.api.InitializerTask;

import hiconic.rx.initializer.api.InitializerBackend;

/** Collects initializer registrations independently of the backend selected by the assembled application. */
public class RxInitializerCoordinator implements InitializerRegistry {

	private record TaskEntry(InitializerFingerprintResolver fingerprintResolver, InitializerTask task) {}
	private record OrderEntry(String runsFirstName, String runsLaterName) {}

	private final Map<String, TaskEntry> tasks = new LinkedHashMap<>();
	private final Set<OrderEntry> orders = new LinkedHashSet<>();
	private InitializerBackend backend;
	private boolean backendExplicitlyConfigured;
	private boolean runningOrFinished;

	public RxInitializerCoordinator(InitializerBackend defaultBackend) {
		backend = defaultBackend;
	}

	@Override
	public synchronized void registerInitializer(String initializerName, InitializerFingerprintResolver fingerprintResolver, InitializerTask task) {
		ensureRegistrationOpen();
		TaskEntry previous = tasks.putIfAbsent(initializerName, new TaskEntry(fingerprintResolver, task));
		if (previous != null)
			throw new IllegalStateException("Initializer task already registered: " + initializerName);
	}

	@Override
	public synchronized void ensureOrder(String runsFirstName, String runsLaterName) {
		ensureRegistrationOpen();
		orders.add(new OrderEntry(runsFirstName, runsLaterName));
	}

	public synchronized void setBackend(InitializerBackend backend) {
		ensureRegistrationOpen();
		if (backendExplicitlyConfigured)
			throw new IllegalStateException("An initializer backend was already explicitly configured");
		this.backend = backend;
		backendExplicitlyConfigured = true;
	}

	public synchronized void runInitializers() {
		if (runningOrFinished)
			throw new IllegalStateException("RX initializers may only be run once");
		runningOrFinished = true;

		tasks.forEach((name, entry) -> backend.registerInitializer(name, entry.fingerprintResolver(), entry.task()));
		orders.forEach(order -> backend.ensureOrder(order.runsFirstName(), order.runsLaterName()));
		backend.runInitializers();
	}

	private void ensureRegistrationOpen() {
		if (runningOrFinished)
			throw new IllegalStateException("Initializer registration is closed because execution has already started");
	}
}
