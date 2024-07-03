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
package hiconic.rx.web.ddra.wire.space;

import static com.braintribe.model.generic.typecondition.TypeConditions.isKind;
import static com.braintribe.model.generic.typecondition.TypeConditions.orTc;

import com.braintribe.model.DdraEndpointDepthKind;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.generic.typecondition.basic.TypeKind;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import dev.hiconic.servlet.ddra.endpoints.api.DdraTraversingCriteriaMap;

@Managed
public class TcSpace implements WireSpace {

	@Managed
	public DdraTraversingCriteriaMap criteriaMap() {
		DdraTraversingCriteriaMap map = new DdraTraversingCriteriaMap();
		map.addDefaultCriterion(DdraEndpointDepthKind.shallow, tcShallow());
		map.addDefaultCriterion(DdraEndpointDepthKind.reachable, tcReachable());
		
		return map;
	}
	
	private TraversingCriterion tcReachable() {
		return TC.create().negation().joker().done();
	}
	
	private TraversingCriterion tcShallow() {
		// @formatter:off
		return TC.create()
			.conjunction()
				.property()
				.typeCondition(orTc(
					isKind(TypeKind.collectionType),
					isKind(TypeKind.entityType)
				))
			.close()
			.done();
		// @formatter:on
	}
}