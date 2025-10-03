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
package hiconic.rx.module.api.wire;

import com.braintribe.codec.marshaller.api.ConfigurableMarshallerRegistry;
import com.braintribe.model.processing.worker.api.ConfigurableWorkerAspectRegistry;
import com.braintribe.model.processing.worker.api.Worker;
import com.braintribe.model.processing.worker.api.WorkerManager;

/**
 * Configurator to override or extend the core platform APIs, available via {@link RxPlatformContract}.
 */
public interface RxPlatformConfigurator {

	/**
	 * Overrides the default {@link WorkerManager}.
	 * <p>
	 * This is useful in a cluster, where we need to provide a WorkerManager which ensures {@link Worker#isSingleton() singleton workers} only run on
	 * one node in a cluster.
	 */
	void setWorkerManager(WorkerManager workerManager);

	ConfigurableMarshallerRegistry marshallerRegistry();

	ConfigurableWorkerAspectRegistry workerAspectRegistry();

}
