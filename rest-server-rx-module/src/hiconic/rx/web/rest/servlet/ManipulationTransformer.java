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
package hiconic.rx.web.rest.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.manipulation.ChangeValueManipulation;
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.InstantiationManipulation;
import com.braintribe.model.generic.manipulation.LocalEntityProperty;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.ManipulationType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.tracking.ManipulationListener;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.generic.value.PersistentEntityReference;
import com.braintribe.model.generic.value.PreliminaryEntityReference;
import com.braintribe.model.processing.session.api.managed.ManipulationMode;
import com.braintribe.model.processing.session.impl.managed.BasicManagedGmSession;

import dev.hiconic.servlet.decoder.api.HttpExceptions;
import hiconic.rx.web.rest.servlet.handlers.AbstractRestV2Handler;

public class ManipulationTransformer {

	private final List<Manipulation> rawManipulations = new ArrayList<>();

	private final List<GenericEntity> entities = new ArrayList<>();

	private final BasicManagedGmSession session = new BasicManagedGmSession();

	private final ManipulationListener listener = this::add;

	private boolean transformingMultipleEntities;

	private boolean transformingMultipleNestedEntities;

	private Long requestEntityRuntimeId;

	public Long getRequestEntityRuntimeId() {
		return requestEntityRuntimeId;
	}

	public void setRequestEntityRuntimeId(Long requestEntityRuntimeId) {
		this.requestEntityRuntimeId = requestEntityRuntimeId;
	}

	public ManipulationTransformer() {
		session.listeners().add(listener);
	}

	public void add(Manipulation manipulation) {
		rawManipulations.add(manipulation);
		if (manipulation.manipulationType() == ManipulationType.INSTANTIATION) {
			entities.add(((InstantiationManipulation) manipulation).getEntity());
		}
	}

	public void applyInducedManipulation(Manipulation inducedManipulation) {
		session.listeners().remove(listener);
		session.manipulate().mode(ManipulationMode.REMOTE).apply(inducedManipulation);
	}

	public List<GenericEntity> getEntities() {
		return entities;
	}

	public List<Manipulation> computeRemoteManipulations(Collection<GenericEntity> entitiesToCreate, boolean allowCreateWithoutId) {
		List<Manipulation> result = new ArrayList<>();

		GenericEntity entity = null;
		InstantiationManipulation instantiation = null;
		EntityReference reference = null;
		boolean isCreatingEntity = false;

		for (Manipulation manipulation : rawManipulations) {
			switch (manipulation.manipulationType()) {
				case INSTANTIATION:
					instantiation = (InstantiationManipulation) manipulation;
					entity = instantiation.getEntity();
					isCreatingEntity = entitiesToCreate.contains(entity);
					reference = getRootReference(entity, allowCreateWithoutId, isCreatingEntity);

					instantiation.setEntity(reference);
					if (isCreatingEntity) {
						result.add(instantiation);
					}
					break;
				case CHANGE_VALUE:
					ChangeValueManipulation changeValue = (ChangeValueManipulation) manipulation;
					LocalEntityProperty localProperty = (LocalEntityProperty) changeValue.getOwner();
					Property property = localProperty.property();
					if (property.isIdentifying() && !entitiesToCreate.contains(localProperty.getEntity())) {
						continue;
					}
					EntityProperty entityProperty = EntityProperty.T.create();
					entityProperty.setPropertyName(localProperty.getPropertyName());
					entityProperty.setReference(
							getRootReference(localProperty.getEntity(), allowCreateWithoutId, entitiesToCreate.contains(localProperty.getEntity())));
					changeValue.setOwner(entityProperty);
					if (property.getType().areEntitiesReachable()) {
						GenericModelType type = localProperty.property().getType();
						Object newValue = changeValue.getNewValue();
						changeValue.setNewValue(getRemotifiedValue(type, newValue));
					}

					result.add(changeValue);
					break;
				default:
					HttpExceptions.throwBadRequest("INSTANTIATION or CHANGE_VALUE only supported.");
			}
		}

		return result;
	}

	protected Object getRemotifiedValue(GenericModelType type, Object value) {
		if (value == null)
			return value;

		switch (type.getTypeCode()) {
			case objectType:
				return getRemotifiedValue(GMF.getTypeReflection().getType(value), value);
			case entityType:
				return AbstractRestV2Handler.entityReference((GenericEntity) value);
			case listType:
			case setType:
				Collection<?> collectionValue = (Collection<?>) value;
				if (collectionValue.isEmpty()) {
					return collectionValue;
				}

				LinearCollectionType collectionType = (LinearCollectionType) type;
				GenericModelType elementType = collectionType.getCollectionElementType();
				Collection<Object> target = collectionType.createPlain();
				for (Object element : collectionValue) {
					target.add(getRemotifiedValue(elementType, element));
				}
				return target;
			case mapType:
				Map<?, ?> mapValue = (Map<?, ?>) value;
				if (mapValue.isEmpty()) {
					return mapValue;
				}
				MapType mapType = (MapType) type;
				Map<Object, Object> targetMap = mapType.createPlain();
				GenericModelType keyType = mapType.getKeyType();
				GenericModelType valueType = mapType.getValueType();
				boolean keyNeedsRemotifying = keyType.areEntitiesReachable();
				boolean valueNeedsRemotifying = valueType.areEntitiesReachable();
				for (Entry<?, ?> entry : mapValue.entrySet()) {
					Object propertyKey = keyNeedsRemotifying ? getRemotifiedValue(keyType, entry.getKey()) : entry.getKey();
					Object propertyValue = valueNeedsRemotifying ? getRemotifiedValue(valueType, entry.getValue()) : entry.getValue();
					targetMap.put(propertyKey, propertyValue);
				}
				return targetMap;
			default:
				return value;
		}
	}

	private EntityReference getRootReference(GenericEntity entity, boolean allowPreliminaryReference, boolean convertToPreliminaryReference) {
		EntityReference reference = AbstractRestV2Handler.entityReference(entity);
		if (reference instanceof PersistentEntityReference) {
			if (convertToPreliminaryReference) {
				PreliminaryEntityReference result = PreliminaryEntityReference.T.create();
				result.setTypeSignature(reference.getTypeSignature());
				result.setPartition(entity.getPartition());
				result.setRefId(entity.runtimeId());
				return result;
			}
			return reference;
		}

		if(allowPreliminaryReference)
			return reference;

		badRequestIdentifierMissing();
		return null;
	}

	public BasicManagedGmSession getSession() {
		return session;
	}

	public boolean isTransformingMultipleEntities() {
		return transformingMultipleEntities;
	}

	public boolean isTransformingMultipleNestedEntities() {
		return transformingMultipleNestedEntities;
	}

	public void setTransformingMultipleEntities(boolean transformingMultipleEntities) {
		this.transformingMultipleEntities = transformingMultipleEntities;
	}

	public void setTransformingMultipleNestedEntities(boolean transformingMultipleNestedEntities) {
		this.transformingMultipleNestedEntities = transformingMultipleNestedEntities;
	}

	private void badRequestIdentifierMissing() {
		HttpExceptions.throwBadRequest("The entity must be fully identified (ID provided) either in the URL path, or in the payload.");
	}
}
