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

import java.util.function.Function;

import com.braintribe.model.openapi.v3_0.JsonReferencable;

/**
 * This trivial implementation of a {@link JsonReferenceBuilder} always creates a new instance instead of storing it in
 * a pool and providing references to it.
 */
public class InliningReferenceBuilder<T extends JsonReferencable, A> implements JsonReferenceBuilder<T, A> {

	private final String key;
	private final A ownContext;
	private T builtEntity;

	public InliningReferenceBuilder(String key, ReferenceRecyclingContext<A> context) {
		this.key = key;
		this.ownContext = context.publicApiContext();
	}

	@Override
	public InliningReferenceBuilder<T, A> ensure(Function<A, T> factory) {
		builtEntity = factory.apply(ownContext);
		return this;
	}

	@Override
	public T getRef() {
		if (builtEntity == null) {
			throw new IllegalStateException("No result found for '" + key + "'. Please call the ensure() method first.");
		}

		return builtEntity;
	}

}
