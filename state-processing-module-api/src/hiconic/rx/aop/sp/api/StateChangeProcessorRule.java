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

import java.util.List;

import com.braintribe.model.generic.GenericEntity;

/**
 * the basic rule that defines a case where a {@link StateChangeProcessor} should be called.
 * <p>
 * additionally, it defines an unique id as a {@link String} for the processor, so the rule can be identify across threads and even processor nodes
 * (other JVMs in case of JMS for instance)
 * 
 * @author pit
 * @author dirk
 */
public interface StateChangeProcessorRule {

	/**
	 * @return - the unique id of the rule the processors are contained within
	 */
	public String getRuleId();

	/**
	 * @return - the processor we need to call
	 */
	public StateChangeProcessor<? extends GenericEntity, ?> getStateChangeProcessor(String processorId);

	public List<StateChangeProcessorMatch> matches(StateChangeProcessorSelectorContext context);
}
