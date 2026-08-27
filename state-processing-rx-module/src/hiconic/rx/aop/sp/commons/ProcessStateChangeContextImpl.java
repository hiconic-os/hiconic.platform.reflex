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
import java.util.Map;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.generic.value.PersistentEntityReference;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

import hiconic.rx.aop.sp.api.ProcessStateChangeContext;

/**
 * a {@link AbstractStateChangeContext} implementation for the actual process state change 
 * 
 * @author pit
 * @author dirk 
 * @param <T> - the process entity
 */
public class ProcessStateChangeContextImpl<T extends GenericEntity> extends AbstractPostStateChangeContext<T> implements ProcessStateChangeContext<T> {
	
	private Map<EntityReference, PersistentEntityReference> referenceMap;
	
	public ProcessStateChangeContextImpl(PersistenceGmSession userSession, PersistenceGmSession systemSession, EntityReference entityReference, EntityProperty entityProperty, Manipulation manipulation) {
		super( userSession, systemSession, entityReference, entityProperty, manipulation);
	}

	public void initInducedManipulationList(List<Manipulation> inducedManipulations) {
		setInducedManipulations( inducedManipulations);
	}
	
	
	
	public List<Manipulation> takeInducedManipulations() {
		return getInducedManipulations();
	}

	public void setReferenceMap(Map<EntityReference, PersistentEntityReference> referenceMap) {
		this.referenceMap = Collections.unmodifiableMap(referenceMap);
	}
	
	@Override
	public Map<EntityReference, PersistentEntityReference> getReferenceMap() {
		return referenceMap;
	}

}
