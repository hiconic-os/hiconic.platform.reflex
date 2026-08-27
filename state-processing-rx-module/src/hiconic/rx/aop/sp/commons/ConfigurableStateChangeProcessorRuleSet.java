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
package hiconic.rx.aop.sp.commons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hiconic.rx.aop.sp.api.StateChangeProcessorException;
import hiconic.rx.aop.sp.api.StateChangeProcessorRule;
import hiconic.rx.aop.sp.api.StateChangeProcessorRuleSet;

/**
 * basic (ioc based) implementation of the {@link StateChangeProcessorRuleSet}<br/>
 * <br/>
 * in order to retain the order of the rules supplied, and to get a quick access to the 
 * stored rule via its processor id, both list and map are sustained.
 *<br/><br/>
 * @author pit
 * @author dirk
 *
 */
public class ConfigurableStateChangeProcessorRuleSet implements StateChangeProcessorRuleSet {

	protected List<StateChangeProcessorRule> rules;
	protected Map<String, StateChangeProcessorRule> idToRuleMap = new HashMap<String, StateChangeProcessorRule>();
	
	public void setProcessorRules(List<StateChangeProcessorRule> rules) {
		this.rules = rules;
		// clear the currently stored rules .. 
		idToRuleMap.clear();
		for (StateChangeProcessorRule rule : rules) {
			idToRuleMap.put( rule.getRuleId(), rule);
		}
	}
	
	@Override
	public List<StateChangeProcessorRule> getProcessorRules() {		
		return rules;
	}

	@Override
	public StateChangeProcessorRule getProcessorRule(String processorId) throws StateChangeProcessorException {
		StateChangeProcessorRule rule = idToRuleMap.get( processorId);
		if (rule != null)
			return rule;
		throw new StateChangeProcessorException("no rule found with processor id [" + processorId + "]");
	}

}
