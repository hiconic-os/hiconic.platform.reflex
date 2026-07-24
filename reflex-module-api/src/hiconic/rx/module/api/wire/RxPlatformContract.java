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

import com.braintribe.wire.api.annotation.Import;

/**
 * Main contract that serves as a library of all contracts {@link Import imported} to any Reflex module.
 * 
 * @see RxExportContract
 * @see EnvironmentPropertiesContract
 * @see SystemPropertiesContract
 * @see RxPropertiesContract
 */
@SuppressWarnings("deprecation")
public interface RxPlatformContract extends DeprecatedRxPlatformContract {

	RxApplicationContract application();

	RxApplicationFilesContract applicationFiles();

	RxAuthContract auth();

	RxConfigurationContract configuration();

	RxExecutionContract execution();

	RxMarshallingContract marshalling();

	RxPackagedResourcesContract packagedResources();

	RxPackagedPublicResourcesContract packagedPublicResources();

	RxProcessLaunchContract processLaunch();

	RxServiceProcessingContract serviceProcessing();

	RxTransientDataContract transientData();

}
