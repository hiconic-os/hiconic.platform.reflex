// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2019 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package hiconic.rx.web.undertow.wire.space;

import static com.braintribe.model.generic.typecondition.TypeConditions.isKind;
import static com.braintribe.model.generic.typecondition.TypeConditions.orTc;

import com.braintribe.ddra.endpoints.api.DdraTraversingCriteriaMap;
import com.braintribe.model.DdraEndpointDepthKind;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.generic.typecondition.basic.TypeKind;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

@Managed
public class TcSpace implements WireSpace {

	@Managed
	public DdraTraversingCriteriaMap criteriaMap() {
		DdraTraversingCriteriaMap map = new DdraTraversingCriteriaMap();
		map.addDefaultCriterion(DdraEndpointDepthKind.shallow, tcShallow());
		map.addDefaultCriterion(DdraEndpointDepthKind.reachable, tcReachable());
		
		return map;
	}
	
	@Managed
	private TraversingCriterion tcReachable() {
		TraversingCriterion bean = TC.create().negation().joker().done();
		return bean;
	}
	
	@Managed
	private TraversingCriterion tcShallow() {
		return TC.create()
		.conjunction()
		.property()
		.typeCondition(orTc(
			isKind(TypeKind.collectionType),
			isKind(TypeKind.entityType)
		))
		.close()
		.done();
	}
}