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
package hiconic.rx.access.module.processing;

import java.util.function.Supplier;

import com.braintribe.model.accessapi.PersistenceRequest;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resourceapi.base.ResourceRequest;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.access.module.wire.space.AccessRxModuleSpace;

public interface RxAccessConstants {

	/**
	 * This model is added to every service domain of every access.
	 * <p>
	 * It binds {@link PersistenceRequest} to {@link RxPersistenceProcessor}
	 * 
	 *  @see RxAccesses#deploy(Access, Supplier)
	 *  @see AccessRxModuleSpace#configurePersistenceProcessor
	 */
	String ACCESS_API_BASE_MODEL_NAME = "reflex:access-api-base-model";

	/**
	 * This model is added to every service domain of every access which contains the {@link Resource} type.
	 * <p>
	 * It binds {@link ResourceRequest} to {@link ResourceRequestProcessor}
	 * 
	 *  @see RxAccesses#deploy(Access, Supplier)
	 *  @see AccessRxModuleSpace#configureResourceRequestProcessor
	 */
	String ACCESS_API_RESOURCE_MODEL_NAME = "reflex:access-api-resource-model";
}
