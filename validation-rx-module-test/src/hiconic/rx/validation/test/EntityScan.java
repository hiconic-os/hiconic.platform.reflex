// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package hiconic.rx.validation.test;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.BaseType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.TypeCode;
import com.braintribe.model.processing.traversing.api.path.TraversingListItemModelPathElement;
import com.braintribe.model.processing.traversing.api.path.TraversingMapKeyModelPathElement;
import com.braintribe.model.processing.traversing.api.path.TraversingMapValueModelPathElement;
import com.braintribe.model.processing.traversing.api.path.TraversingModelPathElement;
import com.braintribe.model.processing.traversing.api.path.TraversingPropertyModelPathElement;
import com.braintribe.model.processing.traversing.api.path.TraversingRootModelPathElement;
import com.braintribe.model.processing.traversing.api.path.TraversingSetItemModelPathElement;

public class EntityScan {
	private final EnqueuedScan deferringAnchor = new EnqueuedScan(null, null, null);
	private EnqueuedScan lastNode = deferringAnchor;
	private boolean directPropertyAccess;
	private EntityScanListener listener;
	private Set<GenericEntity> visited = Collections.newSetFromMap(new IdentityHashMap<GenericEntity, Boolean>());
	
	public EntityScan(EntityScanListener listener) {
		super();
		this.listener = listener;
	}

	public void setDirectPropertyAccess(boolean directPropertyAccess) {
		this.directPropertyAccess = directPropertyAccess;
	}
	
	public void visit(Object value) {
		visit(BaseType.INSTANCE, value);
	}
	
	public void visit(GenericModelType type, Object value) {
		GenericModelType actualType = type.getActualType(value);
		TraversingRootModelPathElement rpe = new TraversingRootModelPathElement(null, value, actualType);
		visit(actualType, value, 0, rpe);
		EnqueuedScan deferringNode = deferringAnchor.next;
		
		while (deferringNode != null) {
			visit(deferringNode.type, deferringNode.value, 0, deferringNode.mpe);
			deferringNode = deferringNode.next;
		}
	}
	
	protected boolean include(GenericModelType type) {
		return type.areEntitiesReachable();
	}
	
	private void visit(GenericModelType type, Object value, int depth, TraversingModelPathElement mpe) {
		if (value == null)
			return;
		
		if (!include(type))
			return;
		
		// break the callstack principle and enqueue
		if (depth > 100) {
			lastNode = lastNode.next = new EnqueuedScan(value, type, mpe);
		}
			
		while(true) {
			TypeCode typeCode = type.getTypeCode();
			
			if (typeCode == null) {
				throw new IllegalStateException("Unexpected actual type of object at traversal depth " + depth + ". No GenericModelType assignable for: '" + value + "'");
			}
			
			switch (typeCode) {
			case objectType:
				type = type.getActualType(value);
				break;
				
			case entityType:
				GenericEntity entity = (GenericEntity)value;
				EntityType<GenericEntity> entityType = entity.entityType();
				
				if (!visited.add(entity))
					return;
				
				listener.visitEntity(entity, entityType, mpe);
				visit(entity, entityType, depth, mpe);
				return;
				
			case listType: 
				GenericModelType listElementType = ((LinearCollectionType)type).getCollectionElementType();
				int i = 0;
				for (Object element: (Collection<?>)value) {
					GenericModelType actualElementType = listElementType.getActualType(element);
					TraversingListItemModelPathElement lpe = new TraversingListItemModelPathElement(mpe, element, actualElementType, i);
					visit(actualElementType, element, depth + 1, lpe);
					
					i++;
				}
				return;
				
			case setType:
				GenericModelType setElementType = ((LinearCollectionType)type).getCollectionElementType();
				for (Object element: (Collection<?>)value) {
					GenericModelType actualElementType = setElementType.getActualType(element);
					TraversingSetItemModelPathElement spe = new TraversingSetItemModelPathElement(mpe, element, setElementType);
					visit(actualElementType, element, depth + 1, spe);
				}
				return;
				
			case mapType:
				MapType mapType = (MapType)type;
				GenericModelType keyType = mapType.getKeyType();
				GenericModelType valueType = mapType.getValueType();
				boolean handleKey = include(keyType);
				boolean handleValue = include(valueType);
				
				for (Map.Entry<?, ?> entry: ((Map<?, ?>)value).entrySet()) {
					Object mapKey = entry.getKey();
					Object mapValue = entry.getValue();
					GenericModelType actualKeyType = keyType.getActualType(mapKey);
					GenericModelType actualValueType = valueType.getActualType(mapValue);
					TraversingMapKeyModelPathElement kpe = new TraversingMapKeyModelPathElement(mpe, mapKey, actualKeyType, mapValue, actualValueType, entry);
					
					if (handleKey)
						visit(keyType, mapKey, depth + 1, kpe);
					
					if (handleValue);
						visit(valueType, mapValue, depth + 1, new TraversingMapValueModelPathElement(kpe));
				}
				return;
				
			default:
				return;
			}
		}
	}

	@SuppressWarnings("unused") 
	protected boolean include(Property property, GenericEntity entity, EntityType<?> entityType) {
		return true;
	}
	
	private void visit(GenericEntity entity, EntityType<?> entityType, int depth, TraversingModelPathElement mpe) {
		if (directPropertyAccess) {
			for (Property property: entityType.getCustomTypeProperties()) {
				Object value = property.getDirectUnsafe(entity);
				
				if (value != null && include(property, entity, entityType)) {
					TraversingPropertyModelPathElement ppe = new TraversingPropertyModelPathElement(mpe, entity, entityType, property, false);
					visit(property.getType(), value, depth + 1, ppe);
				}
			}
		}
		else {
			for (Property property: entityType.getCustomTypeProperties()) {
				Object value = property.get(entity);
				
				if (value != null && include(property, entity, entityType)) {
					TraversingPropertyModelPathElement ppe = new TraversingPropertyModelPathElement(mpe, entity, entityType, property, false);
					visit(property.getType(), value, depth + 1, ppe);
				}
			}
		}
	}
	
	protected static class EnqueuedScan {
		public EnqueuedScan next;
		public Object value;
		public GenericModelType type;
		public TraversingModelPathElement mpe;
		
		public EnqueuedScan(Object value, GenericModelType type, TraversingModelPathElement mpe) {
			super();
			this.value = value;
			this.type = type;
			this.mpe = mpe;
		}
	}
	
}
