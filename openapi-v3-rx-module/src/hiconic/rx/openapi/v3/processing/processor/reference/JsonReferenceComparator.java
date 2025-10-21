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
package hiconic.rx.openapi.v3.processing.processor.reference;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.LinearCollectionType;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.openapi.v3_0.JsonReferencable;

import hiconic.rx.openapi.v3.processing.processor.reference.JsonReferenceOptimizer.ProcessingStatus;

/**
 * This class can compare two entities A and B of the same type, where A was created with an ancestor context of the
 * context that created B. If they are equal (by comparing transitively their property tree, caching already compared
 * ones) that means that the ancestor entity can be used as final result.
 *
 * @author Neidhart.Orlich
 * @see ReferenceRecycler
 */
public class JsonReferenceComparator {

	private final Function<JsonReferencable, ProcessingStatus> statusResolver;

	public JsonReferenceComparator(Function<JsonReferencable, ProcessingStatus> statusResolver) {
		this.statusResolver = statusResolver;
	}

	/**
	 * This compares actual entities
	 */
	public <T extends GenericEntity> boolean preferAncestor(T ancestorEntity, T newEntity) {
		for (Property p : ancestorEntity.entityType().getProperties()) {
			Object ancestorValue = p.get(ancestorEntity);
			Object newValue = p.get(newEntity);
			GenericModelType type = p.getType();

			if (!compareProperty(ancestorValue, newValue, type)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * This method compares references that are already resolved by the optimizer. This should in almost all cases be
	 * the case except when
	 * <li>They are the currently compared main entities (this method won't be called for them)
	 * <li>A cycle was detected
	 * <li>ensure(..) created actual impls instead of refs - this should be avoided
	 */
	private boolean compareReferences(JsonReferencable ancestorEntity, JsonReferencable newEntity) {

		ProcessingStatus processingStatus = statusResolver.apply(newEntity);

		if (processingStatus == null) {
			return preferAncestor(ancestorEntity, newEntity);
		}

		if (processingStatus.statusKnown()) {
			return processingStatus == ProcessingStatus.unchanged;
		}

		if (processingStatus == ProcessingStatus.notPresent) {
			return true;
		}

		if (processingStatus == ProcessingStatus.cycleDetected) {
			// Actually at this point it is still undecided whether to use the ancestor or not.
			// Because the algorithm is optimistic we return true, but if any element of the cycle is different
			// from its ancestor, this will effectively be changed later and also for this entity the ancestor will be
			// used.
			return true;
		}

		throw new IllegalStateException("When does this happen");
	}

	private boolean compareProperty(Object ancestorValue, Object newValue, GenericModelType type) {
		if (ancestorValue == null || newValue == null) {
			return ancestorValue == newValue;
		}

		switch (type.getTypeCode()) {
			case entityType:
				if (JsonReferencable.T.isAssignableFrom(type)) {
					return compareReferences((JsonReferencable) ancestorValue, (JsonReferencable) newValue);
				}
				return preferAncestor((GenericEntity) ancestorValue, (GenericEntity) newValue);
			case mapType:
				return compareMap((MapType) type, (Map<?, ?>) ancestorValue, (Map<?, ?>) newValue);
			case setType:
			case listType:
				return compareLinearCollection((LinearCollectionType) type, (Collection<?>) ancestorValue, (Collection<?>) newValue);
			default:
				return ancestorValue.equals(newValue);
		}

	}

	/**
	 * It is assumed that the collection has a determined order even when it is a set (LinkedHashSet). This is currently
	 * the case in our OpenApi implementation
	 */
	private boolean compareLinearCollection(LinearCollectionType collectionType, Collection<?> ancestorCollection, Collection<?> newCollection) {
		if (ancestorCollection.size() != newCollection.size()) {
			return false;
		}

		Iterator<?> it1 = ancestorCollection.iterator();
		Iterator<?> it2 = newCollection.iterator();

		while (it1.hasNext()) {
			Object ancestorValue = it1.next();
			Object newValue = it2.next();

			if (compareProperty(ancestorValue, newValue, collectionType.getCollectionElementType()) == false) {
				return false;
			}
		}

		return true;
	}

	/**
	 * It is assumed that the map has a determined iteration order (LinkedHashMap). This is currently the case in our
	 * OpenApi implementation
	 */
	private boolean compareMap(MapType mapType, Map<?, ?> ancestorMap, Map<?, ?> newMap) {
		if (ancestorMap.size() != newMap.size()) {
			return false;
		}

		for (Object key : ancestorMap.keySet()) {
			Object ancestorValue = newMap.get(key);
			Object newValue = ancestorMap.get(key);

			if (compareProperty(newValue, ancestorValue, mapType.getValueType()) == false) {
				return false;
			}
		}

		return true;
	}

}
