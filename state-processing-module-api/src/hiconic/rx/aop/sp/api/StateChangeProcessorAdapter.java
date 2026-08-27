// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.aop.sp.api;

import com.braintribe.model.generic.GenericEntity;

public class StateChangeProcessorAdapter<T extends GenericEntity, C extends GenericEntity> implements StateChangeProcessor<T, C> {

	private final String processorId;

	public StateChangeProcessorAdapter(String processorId) {
		this.processorId = processorId;
	}

	@Override
	public String processorId() {
		return processorId;
	}

	@Override
	public C onBeforeStateChange(BeforeStateChangeContext<T> context) throws StateChangeProcessorException {
		return null;
	}

	@Override
	public void onAfterStateChange(AfterStateChangeContext<T> context, C customContext) throws StateChangeProcessorException {
		// blank
	}

	@Override
	public void processStateChange(ProcessStateChangeContext<T> context, C customContext) throws StateChangeProcessorException {
		// blank
	}
}
