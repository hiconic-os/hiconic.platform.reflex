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
package hiconic.rx.aop.sp.rule;

import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.util.Collections;
import java.util.List;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.generic.GenericEntity;

import hiconic.rx.aop.sp.api.StateChangeProcessor;
import hiconic.rx.aop.sp.api.StateChangeProcessorMatch;
import hiconic.rx.aop.sp.api.StateChangeProcessorRule;
import hiconic.rx.aop.sp.api.StateChangeProcessorSelectorContext;
import hiconic.rx.aop.sp.commons.StateChangeProcessorMatchImpl;
import hiconic.rx.aop.sp.model.meta.OnChange;
import hiconic.rx.aop.sp.model.meta.OnCreate;
import hiconic.rx.aop.sp.model.meta.OnDelete;
import hiconic.rx.aop.sp.model.meta.StateChangeProcessorConfiguration;
import hiconic.rx.aop.sp.registry.StateProcessorRegistryImpl;

public class MetaDataStateChangeProcessorRule implements StateChangeProcessorRule {

	private String ruleId = MetaDataStateChangeProcessorRule.class.getName();
	private StateProcessorRegistryImpl stateProcessorRegistry;

	@Required
	public void setStateProcessorRegistry(StateProcessorRegistryImpl stateProcessorRegistry) {
		this.stateProcessorRegistry = stateProcessorRegistry;
	}

	@Configurable
	public void setRuleId(String ruleId) {
		this.ruleId = ruleId;
	}

	@Override
	public String getRuleId() {
		return ruleId;
	}

	@Override
	public StateChangeProcessor<? extends GenericEntity, ? extends GenericEntity> getStateChangeProcessor(String processorId) {
		return stateProcessorRegistry.getStateChangeProcessor(processorId);
	}

	@Override
	public List<StateChangeProcessorMatch> matches(StateChangeProcessorSelectorContext context) {
		List<StateChangeProcessorMatch> result = newList();

		List<? extends StateChangeProcessorConfiguration> processorConfigurations = resolveProcessorConfigurations(context);
		for (StateChangeProcessorConfiguration processorConfiguration : processorConfigurations) {
			StateChangeProcessor<?, ?> stateChangeProcessor = resolveStateChangeProcessor(processorConfiguration);

			StateChangeProcessorMatchImpl match = new StateChangeProcessorMatchImpl(stateChangeProcessor);
			result.add(match);
		}

		return result;
	}

	private StateChangeProcessor<?, ?> resolveStateChangeProcessor(StateChangeProcessorConfiguration configuration) {
		StateChangeProcessor<?, ?> result = configuration.getProcessor();
		if (result != null) {
			// Also register the processor so it can be retrieved by id, e.g. from AbstractSpInvocation
			if (configuration.getProcessorId() == null) {
				stateProcessorRegistry.registerStateChangeProcessor(result);
				configuration.setProcessorId(result.processorId());
			}

			return result;
		}

		String processorId = configuration.getProcessorId();
		result = getStateChangeProcessor(processorId);
		configuration.setProcessor(result);

		return result;
	}

	private List<? extends StateChangeProcessorConfiguration> resolveProcessorConfigurations(StateChangeProcessorSelectorContext context) {
		switch (context.getManipulation().manipulationType()) {
			case INSTANTIATION:
				// retrieve meta data for instantiation of an entity
				return context.getCmdResolver().getMetaData().entityType(context.getEntityType()).meta(OnCreate.T).list();

			case ADD:
			case REMOVE:
			case CHANGE_VALUE:
			case CLEAR_COLLECTION:
				// retrieve meta data change of the relevant property
				return context.getCmdResolver().getMetaData().entityType(context.getEntityType())
						.property(context.getEntityProperty().getPropertyName()).meta(OnChange.T).list();

			case DELETE:
				// retrieve meta data for deletion of an entity
				return context.getCmdResolver().getMetaData().entityType(context.getEntityType()).meta(OnDelete.T).list();

			default:
				return Collections.emptyList();
		}
	}
}
