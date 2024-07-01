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
package hiconic.rx.hibernate.service.impl;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.annotations.TransactionMode;
import hiconic.rx.hibernate.service.api.PersistenceDispatchConfiguration;
import hiconic.rx.hibernate.service.api.PersistenceDispatching;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;

public class ConfigurableDispatchingPersistenceServiceProcessor<I extends ServiceRequest, O> extends AbstractDispatchingPersistenceServiceProcessor<I, O> implements PersistenceDispatchConfiguration<I, O> {
	
	
	public ConfigurableDispatchingPersistenceServiceProcessor(PersistenceDispatching<I, O> dispatching) {
		dispatching.bind(dispatchMap);
	}
	
	public ConfigurableDispatchingPersistenceServiceProcessor() {
	}
	
	@Override
	protected void configureDispatching(PersistenceDispatchConfiguration<I, O> dispatch) {
		// empty
	}
	
	@Override
	public <R extends I, AO extends O> void register(EntityType<R> requestType, TransactionMode transactionMode,
			PersistenceServiceProcessor<R, AO> processor) {
		dispatchMap.register(requestType, transactionMode, processor);
	}
}


