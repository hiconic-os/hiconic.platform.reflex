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
package hiconic.rx.initializer.api;

import com.braintribe.gm.initializer.api.InitializerFingerprintResolver;
import com.braintribe.gm.initializer.api.InitializerRegistry;
import com.braintribe.gm.initializer.api.InitializerTask;

/** State and coordination backend used by the RX platform's initializer facility. */
public interface InitializerBackend extends InitializerRegistry {

	void runInitializers();

	static InitializerBackend of(InitializerRegistry registry, Runnable execution) {
		return new InitializerBackend() {
			@Override
			public void registerInitializer(String initializerName, InitializerFingerprintResolver fingerprintResolver, InitializerTask task) {
				registry.registerInitializer(initializerName, fingerprintResolver, task);
			}

			@Override
			public void ensureOrder(String runsFirstName, String runsLaterName) {
				registry.ensureOrder(runsFirstName, runsLaterName);
			}

			@Override
			public void runInitializers() {
				execution.run();
			}
		};
	}
}
