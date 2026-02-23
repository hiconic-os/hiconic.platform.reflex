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
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.worker.api.ConfigurableWorkerAspectRegistry;
import com.braintribe.model.processing.worker.api.Worker;
import com.braintribe.model.processing.worker.api.WorkerManager;
import com.braintribe.model.resource.source.ResourceSource;

import hiconic.rx.module.api.resource.ResourceStorageDeploymentExpert;
import hiconic.rx.resource.model.configuration.ResourceStorage;

/**
 * Configurator to override or extend the core platform APIs.
 * <p>
 * Passed to each module's during platform configuration phase via {@link RxModuleContract#configurePlatform(RxPlatformConfigurator)}.
 */
public interface RxPlatformConfigurator {

	/**
	 * Overrides the default {@link WorkerManager}.
	 * <p>
	 * This is useful in a cluster, where we need to provide a WorkerManager which ensures {@link Worker#isSingleton() singleton workers} only run on
	 * one node in a cluster.
	 * <p>
	 * Configured manager is accessible via {@link RxPlatformContract#workerManager()}
	 */
	void setWorkerManager(WorkerManager workerManager);

	ConfigurableMarshallerRegistry marshallerRegistry();

	ConfigurableWorkerAspectRegistry workerAspectRegistry();

	<RS extends ResourceStorage> void registerResourceStorageDeploymentExpert( //
			EntityType<RS> storageType, //
			EntityType<? extends ResourceSource> sourceType, //
			ResourceStorageDeploymentExpert<RS> expert);

}
