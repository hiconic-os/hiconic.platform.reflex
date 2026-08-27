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
package hiconic.rx.aop.sp.commons.selector;

import java.util.List;

import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.typecondition.TypeCondition;

import hiconic.rx.aop.sp.api.StateChangeProcessorSelectorContext;
import hiconic.rx.aop.sp.api.selector.ConjunctionManipulationSelector;
import hiconic.rx.aop.sp.api.selector.DisjunctionManipulationSelector;
import hiconic.rx.aop.sp.api.selector.ManipulationSelector;
import hiconic.rx.aop.sp.api.selector.ManipulationTypeSelector;
import hiconic.rx.aop.sp.api.selector.NegationManipulationSelector;
import hiconic.rx.aop.sp.api.selector.PropertyNameSelector;
import hiconic.rx.aop.sp.api.selector.TargetTypeSelector;

public class ManipulationSelectorEvaluator {
		
	public static boolean matches( StateChangeProcessorSelectorContext context, ManipulationSelector manipulationSelector) {
		if (manipulationSelector instanceof PropertyNameSelector) {
			PropertyNameSelector propertyNameSelector = (PropertyNameSelector) manipulationSelector;
			String propertyName = propertyNameSelector.getPropertyName();
			if (propertyName == null) {
				return true;
			}
			Property property = context.getProperty();
			if (property != null)
				return propertyName.matches( property.getName());
			return false;
		}
		else if (manipulationSelector instanceof TargetTypeSelector) {
			TargetTypeSelector targetTypeSelector = (TargetTypeSelector) manipulationSelector;
			TypeCondition typeCondition = targetTypeSelector.getTypeCondition();
			if (typeCondition == null)
				return true;
			else
				return typeCondition.matches(context.getEntityType());
		}
		else if (manipulationSelector instanceof ManipulationTypeSelector) {
			ManipulationTypeSelector manipulationTypeSelector = (ManipulationTypeSelector) manipulationSelector;
			TypeCondition typeCondition = manipulationTypeSelector.getTypeCondition();
			if (typeCondition == null)
				return true;
			else
				return typeCondition.matches(context.getManipulation().entityType());
		} 
		else if (manipulationSelector instanceof ConjunctionManipulationSelector) {
			ConjunctionManipulationSelector conjunctionManipulationSelector = (ConjunctionManipulationSelector) manipulationSelector;
			List<ManipulationSelector> operands = conjunctionManipulationSelector.getOperands();
			if (operands != null) {
				for (ManipulationSelector suspect : operands) {
					if (matches( context, suspect) == false)
						return false;
				}
			}
			return true;
		} 
		else if (manipulationSelector instanceof DisjunctionManipulationSelector) {
			DisjunctionManipulationSelector disjunctionManipulationSelector = (DisjunctionManipulationSelector) manipulationSelector;
			List<ManipulationSelector> operands = disjunctionManipulationSelector.getOperands();
			if (operands != null) {
				for (ManipulationSelector suspect : operands) {
					if (matches( context, suspect) == true)
						return true;
				}
			}
			return false;
		}
		else if (manipulationSelector instanceof NegationManipulationSelector) {
			NegationManipulationSelector negationManipulationSelector = (NegationManipulationSelector) manipulationSelector;
			ManipulationSelector operand = negationManipulationSelector.getOperand();
			if (operand != null)
				return !matches( context, operand);
			return false;
		} else {
			throw new UnsupportedOperationException( "the manipulaton selector type [" + manipulationSelector.getClass().getName() + "] is not supported");
		}				
	}
}
