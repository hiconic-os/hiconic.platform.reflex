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
package hiconic.rx.model.service.processing.md;

import com.braintribe.model.generic.annotation.Transient;
import com.braintribe.model.generic.annotation.meta.Description;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

import hiconic.rx.module.api.resource.ResourceStorage;

@Description("Specifies ResourceStorage for given ResourceSource type.")
public interface StoreWith extends EntityTypeMetaData {

	EntityType<StoreWith> T = EntityTypes.T(StoreWith.class);

	/**
	 * {@link ResourceStorage#storageId() storageId} of the actual {@link ResourceStorage} implementation.
	 * <p>
	 * Use this when actual instance is not yet available during model configuration.
	 * <p>
	 * Either this or {@link #getResourceStorage()} must be set.
	 */
	String getStorageId();
	void setStorageId(String storageId);

	/**
	 * Actual {@link ResourceStorage} implementation.
	 * <p>
	 * Either this or {@link #getStorageId()} must be set.
	 * <p>
	 * NOTE if not configured, it will be assigned by the framework first time it revolves {@link #getStorageId()}.
	 */
	@Transient
	ResourceStorage getResourceStorage();
	void setResourceStorage(ResourceStorage associate);

	static StoreWith create(ResourceStorage serviceProcessor) {
		StoreWith processWith = StoreWith.T.create();
		processWith.setResourceStorage(serviceProcessor);
		return processWith;
	}
	
	static StoreWith create(String storageId) {
		StoreWith processWith = StoreWith.T.create();
		processWith.setStorageId(storageId);
		return processWith;
	}

}
