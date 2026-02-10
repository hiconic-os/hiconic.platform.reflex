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
package hiconic.rx.resource.model.api;

import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.service.api.ServiceRequest;

public interface StoreResourcePayload extends ResourcePayloadRequest {

	EntityType<StoreResourcePayload> T = EntityTypes.T(StoreResourcePayload.class);

	/** Data to store, typically a {@link Resource#createTransient transient resource}. */
	@Mandatory
	Resource getData();
	void setData(Resource data);

	/**
	 * Optional storageId of the ResourceStorage to use for storing this resource.
	 * <p>
	 * If set, the {@link #getSourceType() sourceType} property is ignored.
	 */
	String getStorageId();
	void setStorageId(String storageId);

	/**
	 * UseCase used when resolving ResourceStorage MD.
	 * <p>
	 * This value is also set as {@link ResourceSource#getUseCase() useCase} of the returned {@link StoreResourcePayloadResponse#getResourceSource()
	 * ResourceSource}.
	 */
	String getUseCase();
	void setUseCase(String useCase);

	/**
	 * Optional type signature of the desired {@link ResourceSource} type.
	 * <p>
	 * Only used for determining the ResourceStorage in case {@link #getStorageId() storageName} is not provided - by considering the StoreWith MD on
	 * given type or picking the default ResourceStorage if MD configured.
	 * <p>
	 * If not value is provided, {@link ResourceSource} is used for MD resolution.
	 */
	String getSourceType();
	void setSourceType(String sourceType);

	@Override
	EvalContext<StoreResourcePayloadResponse> eval(Evaluator<ServiceRequest> evaluator);

}
