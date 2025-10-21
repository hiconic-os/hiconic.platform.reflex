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

import java.util.Map;
import java.util.function.Function;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.openapi.v3_0.JsonReferencable;

/**
 * This simple implementation of a {@link JsonReferenceBuilder} stores a component into an entity pool and then provides
 * a reference to it without any further optimizations.
 *
 * @see JsonReferenceBuilder
 * @author Neidhart.Orlich
 *
 */
public abstract class NonOptimizingReferenceBuilder<T extends JsonReferencable, A> implements JsonReferenceBuilder<T, A> {

	private final Map<String, T> entityPool;
	private final String refPrefix;
	protected final A ownContext;
	private final T ref;

	/**
	 * @param type
	 *            Entity type of the entities this builder builds.
	 * @param entityPool
	 *            Pool where the actual entities get stored (not their references)
	 * @param poolReferenceKey
	 *            JSON Reference key to the entityPool
	 * @param context
	 *            context that should be passed to {@link #ensure(Function)} to build the actual entity
	 */
	public NonOptimizingReferenceBuilder(EntityType<T> type, Map<String, T> entityPool, String poolReferenceKey,
			ReferenceRecyclingContext<A> context) {
		this.ref = type.create();
		this.entityPool = entityPool;
		this.ownContext = context.publicApiContext();

		refPrefix = poolReferenceKey + "/";
	}

	@Override
	public NonOptimizingReferenceBuilder<T, A> ensure(Function<A, T> factory) {

		String componentKey = getRefKey();
		
		entityPool.computeIfAbsent(componentKey, k -> factory.apply(ownContext));
		
		ref.set$ref(refPrefix + componentKey);

		return this;
	}

	@Override
	public T getRef() {
		if (ref.get$ref() == null) {
			throw new IllegalStateException("Component '" + refPrefix + getRefKey() + "' must be ensured before a reference can be used.");
		}

		return ref;
	}

	public abstract String getRefKey();

}
