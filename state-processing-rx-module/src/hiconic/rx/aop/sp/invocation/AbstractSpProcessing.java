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
package hiconic.rx.aop.sp.invocation;

import com.braintribe.logging.Logger;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.session.exception.GmSessionException;

import hiconic.rx.aop.sp.api.ProcessStateChangeContext;
import hiconic.rx.aop.sp.api.StateChangeProcessor;
import hiconic.rx.aop.sp.api.StateChangeProcessorException;

/**
 * processes a call of a state processor
 * 
 * @author pit
 * @author dirk
 *
 */
public abstract class AbstractSpProcessing {

	private static Logger log = Logger.getLogger(AbstractSpProcessing.class);

	/**
	 * synchronously (directly) process a state change processor
	 * 
	 * @param processorId
	 *            - the id of the processor (only needed for error message purposes)
	 * @param processor
	 *            - the processor to call
	 * @param stateChangeContext
	 *            - the associated context
	 * @param customData
	 *            - the data packet that the processor generated in its {@link StateChangeProcessor#onBeforeStateChange}, if any
	 */
	protected void process(String ruleId, String processorId, StateChangeProcessor<GenericEntity, GenericEntity> processor,
			ProcessStateChangeContext<GenericEntity> stateChangeContext, GenericEntity customData) {

		try {
			processor.processStateChange(stateChangeContext, customData);
			stateChangeContext.commitIfNecessary();

		} catch (StateChangeProcessorException e) {
			String msg = "error while executing process of processor [" + processorId + "] of rule [" + ruleId + "]";
			log.error(msg, e);

		} catch (GmSessionException e) {
			String msg = "error while executing process of processor [" + processorId + "] of rule [" + ruleId + "]";
			log.error(msg, e);
		}
	}
}
