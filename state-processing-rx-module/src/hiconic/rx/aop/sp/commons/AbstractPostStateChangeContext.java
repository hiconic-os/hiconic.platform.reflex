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
import java.util.List;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

import hiconic.rx.aop.sp.api.PostStateChangeContext;

public abstract class AbstractPostStateChangeContext<T extends GenericEntity> extends AbstractStateChangeContext<T> implements PostStateChangeContext<T> {
	
	public AbstractPostStateChangeContext(PersistenceGmSession userSession,
			PersistenceGmSession systemSession, EntityReference entityReference, EntityProperty entityProperty,
			Manipulation manipulation) {
		super(userSession, systemSession, entityReference, entityProperty, manipulation);		
	}

	private List<Manipulation> inducedManipulations;
	
	public void setInducedManipulations(List<Manipulation> inducedManipulations) {
		this.inducedManipulations = inducedManipulations;
	}	
	
	public List<Manipulation> getInducedManipulations() {
		if (inducedManipulations == null) {
			inducedManipulations = new ArrayList<Manipulation>();
		}
		return inducedManipulations;
	}

	@Override
	public void notifyInducedManipulation(Manipulation manipulation) {
		getInducedManipulations().add(manipulation);
		
	}
	
}
