// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.test.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import hiconic.rx.platform.RxPlatform;

/**
 * Explicit JUnit 4 class fixture for RX tests whose methods can safely share one platform.
 * <p>
 * Sharing is deliberately opt-in. Tests which require a fresh bootstrap or which leave incompatible
 * persisted/runtime state must keep using {@link AbstractRxTest} or provide focused cleanup.
 */
public final class RxPlatformTestClassRule implements TestRule {

	private final String appDir;
	private final String applicationName;
	private final Map<String, Supplier<String>> managedPropertySuppliers = new LinkedHashMap<>();
	private RxPlatform platform;

	public RxPlatformTestClassRule(String appDir, String applicationName) {
		this.appDir = appDir;
		this.applicationName = applicationName;
	}

	public RxPlatformTestClassRule withManagedProperty(String name, Supplier<String> valueSupplier) {
		managedPropertySuppliers.put(name, valueSupplier);
		return this;
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				start();
				try {
					base.evaluate();
				} finally {
					close();
				}
			}
		};
	}

	public RxPlatform platform() {
		if (platform == null)
			throw new IllegalStateException("RX platform class fixture is not active");
		return platform;
	}

	private void start() {
		Map<String, String> managedProperties = new LinkedHashMap<>();
		managedPropertySuppliers.forEach((name, supplier) -> managedProperties.put(name, supplier.get()));
		platform = AbstractRxTest.loadPlatform(appDir, applicationName, managedProperties);
	}

	private void close() {
		if (platform == null)
			return;
		try {
			platform.close();
		} finally {
			platform = null;
		}
	}
}
