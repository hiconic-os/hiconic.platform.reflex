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
package hiconic.rx.platform.wire.contract;

import com.braintribe.model.processing.service.api.ProcessorRegistry;

import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.module.wire.space.CoreRxPlatformModuleSpace;
import hiconic.rx.platform.resource.ResourcePayloadProcessor;
import hiconic.rx.platform.resource.RxResourcesStorages;

/**
 * This extension of {@link RxPlatformContract} is used internally to pass certain beans from the platform to the {@link CoreRxPlatformModuleSpace
 * core module}.
 * <p>
 * The reason to add this was registration of {@link ResourcePayloadProcessor}, which has a dependency on platform's internal registry -
 * {@link RxResourcesStorages} - but which we want to register in a standard way via
 * {@link RxModuleContract#registerFallbackProcessors(ProcessorRegistry)} rather than creating additional steps in the platform itself.
 */
public interface ExtendedRxPlatformContract extends RxPlatformContract {

	RxResourcesStorages resourceStorages();

}
