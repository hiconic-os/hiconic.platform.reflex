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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.braintribe.cfg.Required;

import hiconic.rx.aop.sp.api.ReflectiveStateChangeProcessorRule;
import hiconic.rx.aop.sp.api.SelectiveStateChangeProcessorAddressing;
import hiconic.rx.aop.sp.api.StateChangeProcessor;
import hiconic.rx.aop.sp.api.StateChangeProcessorMapping;
import hiconic.rx.aop.sp.api.StateChangeProcessorMatch;
import hiconic.rx.aop.sp.api.StateChangeProcessorSelectorContext;
import hiconic.rx.aop.sp.commons.selector.ManipulationSelectorEvaluator;

public class ConfigurableStateChangeProcessorRule implements ReflectiveStateChangeProcessorRule {

	private String ruleId;
	private List<StateChangeProcessorMapping> processorMappings;
	private Map<String, StateChangeProcessorMapping> processorMappingByProcessorId;

	@Required
	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	@Required
	public void setProcessorMappings(List<StateChangeProcessorMapping> processorMappings) {
		this.processorMappings = processorMappings;
		processorMappingByProcessorId = new HashMap<>();
		for (StateChangeProcessorMapping mapping : processorMappings)
			processorMappingByProcessorId.put(mapping.getProcessorAddressing().getProcessorId(), mapping);
	}

	@Override
	public String getRuleId() {
		return ruleId;
	}

	@Override
	public StateChangeProcessor<?, ?> getStateChangeProcessor(String processorId) {
		StateChangeProcessor<?, ?> processor = (StateChangeProcessor<?, ?>) processorMappingByProcessorId.get(processorId);
		return processor;
	}

	@Override
	public List<StateChangeProcessorMatch> matches(StateChangeProcessorSelectorContext context) {
		List<StateChangeProcessorMatch> matches = new ArrayList<>();

		for (StateChangeProcessorMapping mapping : processorMappings) {
			SelectiveStateChangeProcessorAddressing addressing = mapping.getProcessorAddressing();
			if (ManipulationSelectorEvaluator.matches(context, addressing.getManipulationSelector())) {
				StateChangeProcessor<?, ?> stateChangeProcessor = mapping.getProcessor();
				StateChangeProcessorMatch match = new StateChangeProcessorMatchImpl(stateChangeProcessor);
				matches.add(match);
			}
		}

		return matches;
	}

	@Override
	public List<StateChangeProcessorMapping> getProcessorMappings() {
		return processorMappings;
	}

}
