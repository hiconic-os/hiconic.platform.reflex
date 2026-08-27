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

import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.manipulation.DeleteManipulation;
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.InstantiationManipulation;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.PropertyManipulation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

import hiconic.rx.aop.sp.api.StateChangeProcessorSelectorContext;

/**
 * implements the {@link StateChangeProcessorSelectorContext}
 * 
 * @author pit
 * @author dirk
 */
public class StateChangeProcessorSelectorContextImpl implements StateChangeProcessorSelectorContext {
	private final Manipulation manipulation;
	private EntityReference entityReference;
	private EntityType<?> entityType;
	private Property property;

	private final PersistenceGmSession session;
	private final PersistenceGmSession systemSession;
	
	private boolean forLifecycle;
	private boolean forDeletion;
	private boolean forInstantiation;
	
	private Object customContext;

	public StateChangeProcessorSelectorContextImpl( PersistenceGmSession session, PersistenceGmSession systemSession, Manipulation manipulation) {
		this.manipulation = manipulation;
		this.session = session;
		this.systemSession = systemSession;
		
		switch (manipulation.manipulationType()) {
		case INSTANTIATION:
			forInstantiation = forLifecycle = true;
			break;
			
		case DELETE:
			forDeletion = forLifecycle = true;
			break;
		
		default:
			break;
		}
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <M extends Manipulation> M getManipulation() {	
		return (M)manipulation;
	}

	@Override
	public EntityReference getEntityReference() {
		if (entityReference == null) {		
			switch (manipulation.manipulationType()) {
			case ADD:
			case REMOVE:
			case CHANGE_VALUE:
			case CLEAR_COLLECTION:
				entityReference = getEntityProperty().getReference();
				break;

			case INSTANTIATION:
				entityReference = (EntityReference) ((InstantiationManipulation)manipulation).getEntity();
				break;
			case DELETE:
				entityReference = (EntityReference) ((DeleteManipulation)manipulation).getEntity();
				break;
			default:
				break;
			
			}
						
		}
		return entityReference;
	}
	
	@Override
	public EntityProperty getEntityProperty() {
		if (isForProperty())
			return (EntityProperty)((PropertyManipulation)manipulation).getOwner();
		else
			return null;
	}

	@Override
	public EntityType<?> getEntityType() {
		if (entityType == null) {	
			entityType = GMF.getTypeReflection().getEntityType(getEntityReference().getTypeSignature());
		}
		return entityType;
	}

	@Override
	public Property getProperty() {
		if (property == null) {
			EntityProperty entityProperty = getEntityProperty();
			if (entityProperty == null)
				return null;
			property = getEntityType().getProperty( entityProperty.getPropertyName());			
		}
		return property;
	}

	@Override
	public CmdResolver getCmdResolver() {
		return session.getModelAccessory().getCmdResolver();
	}

	@Override
	public PersistenceGmSession getSession() {	
		return session;
	}

	@Override
	public PersistenceGmSession getSystemSession() {
		return systemSession;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCustomContext() {
		return (T) customContext;
	}
	
	@Override
	public void setCustomContext(Object customContext) {
		this.customContext = customContext;
	}
	
	@Override
	public boolean isForDeletion() {
		return forDeletion;
	}
	
	@Override
	public boolean isForInstantiation() {
		return forInstantiation;
	}

	@Override
	public boolean isForLifecycle() {
		return forLifecycle;
	}
	
	@Override
	public boolean isForProperty() {
		return !forLifecycle;
	}
	
	
}
