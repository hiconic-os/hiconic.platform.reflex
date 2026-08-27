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
package hiconic.rx.aop.sp.model.meta;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.annotation.Transient;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

import hiconic.rx.aop.sp.api.StateChangeProcessor;

@Abstract
public interface StateChangeProcessorConfiguration extends GenericEntity {

	EntityType<StateChangeProcessorConfiguration> T = EntityTypes.T(StateChangeProcessorConfiguration.class);

	/**
	 * {@link StateChangeProcessor#processorId() processorId} of the actual {@link StateChangeProcessor} implementation.
	 * <p>
	 * Use this when actual instance is not yet available during model configuration.
	 * <p>
	 * Either this or {@link #getProcessor()} must be set.
	 */
	String getProcessorId();
	void setProcessorId(String processorId);

	/**
	 * Actual {@link StateChangeProcessor} implementation.
	 * <p>
	 * Either this or {@link #getProcessorId()} must be set.
	 * <p>
	 * NOTE if not configured, it will be assigned by the framework first time it revolves {@link #getProcessorId()}.
	 */
	@Transient
	StateChangeProcessor<?, ?> getProcessor();
	void setProcessor(StateChangeProcessor<?, ?> processor);

}
