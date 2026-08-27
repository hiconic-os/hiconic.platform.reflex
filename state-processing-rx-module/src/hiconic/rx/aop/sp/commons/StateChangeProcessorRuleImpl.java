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

import java.util.Collections;
import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.typecondition.TypeCondition;

import hiconic.rx.aop.sp.api.StateChangeProcessor;
import hiconic.rx.aop.sp.api.StateChangeProcessorMatch;
import hiconic.rx.aop.sp.api.StateChangeProcessorRule;
import hiconic.rx.aop.sp.api.StateChangeProcessorSelectorContext;


/**
 * implements both {@link StateChangeProcessorRule} and {@link StateChangeProcessorMatch} <br/>
 * <br/>
 * {@link #matches(StateChangeProcessorSelectorContext)} tests on the correct entity type and property name
 * 
 * @author pit
 * @author dirk
 *
 */
public class StateChangeProcessorRuleImpl implements StateChangeProcessorRule, StateChangeProcessorMatch {

	private StateChangeProcessor<? extends GenericEntity, ? extends GenericEntity> stateChangeProcessor;
	private String stateProperty;
	private TypeCondition typeCondition;
	private String processorId;
	
	@Override
	public StateChangeProcessor<? extends GenericEntity, ? extends GenericEntity> getStateChangeProcessor() {
		return stateChangeProcessor;
	}
	
	public void setStateChangeProcessor( StateChangeProcessor<? extends GenericEntity, ? extends GenericEntity> stateChangeProcessor) {
		this.stateChangeProcessor = stateChangeProcessor;
	}
	
	public String getStateProperty() {	
		return stateProperty;
	}
	public void setStateProperty(String stateProperty) {
		this.stateProperty = stateProperty;
	}

	public TypeCondition getInstanceTypeCondition() {		
		return typeCondition;
	}
	public void setTypeCondition(TypeCondition typeCondition) {
		this.typeCondition = typeCondition;
	}
	
			
	@Override
	public String getRuleId() {
		return processorId;
	}
	
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}

	@Override
	public String getProcessorId() {		
		return processorId;
	}

	@Override
	public StateChangeProcessor<? extends GenericEntity, ? extends GenericEntity> getStateChangeProcessor( String processorId) {
		return stateChangeProcessor;
	}

	@Override
	public List<StateChangeProcessorMatch> matches(StateChangeProcessorSelectorContext context) {
		if	(matchesPropertyCondition(context) && matchesTypeCondition(context)) {
			return Collections.<StateChangeProcessorMatch>singletonList( this);
		} else {
			return Collections.emptyList();
		}
	}

	private boolean matchesTypeCondition(StateChangeProcessorSelectorContext context) {
		if (typeCondition == null)
			return true;
		else
			return typeCondition.matches(context.getEntityType());
	}

	private boolean matchesPropertyCondition(StateChangeProcessorSelectorContext context) {
		if (!context.isForProperty())
			return false;
		
		String statePropertyName = getStateProperty();
		
		if (statePropertyName == null)
			return true;
		
		String propertyName = context.getProperty().getName();
		return propertyName.equals(statePropertyName);
	}
}
