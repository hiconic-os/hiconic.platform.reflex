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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ReferenceRecyclingContext<A> {

	private final Map<String, ReferenceRecyclingContext<A>> knownChildContexts = new HashMap<>();

	private final JsonReferenceOptimizer<A> jsonReferenceOptimizer;
	private final ReferenceRecyclingContext<A> parentContext;
	private final String keySuffix;
	private final A publicApiContext;
	private final Function<ReferenceRecyclingContext<A>, A> publicApiContextFactory;
	private boolean sealed;

	public ReferenceRecyclingContext(ReferenceRecyclingContext<A> parentContext, String keySuffix,
			Function<ReferenceRecyclingContext<A>, A> publicApiContextFactory) {
		this.parentContext = parentContext;
		this.publicApiContextFactory = publicApiContextFactory;
		this.keySuffix = normalizeKeySuffix(keySuffix);

		this.jsonReferenceOptimizer = new JsonReferenceOptimizer<>(this);
		this.publicApiContext = publicApiContextFactory.apply(this);
	}
	
	private static String normalizeKeySuffix(String keySuffix) {
		if(keySuffix == null)
			return "";

		if (keySuffix.startsWith("-"))
			return keySuffix;
		
		return "-" + keySuffix;
	}

	public JsonReferenceOptimizer<A> getOptimizationResults() {
		return jsonReferenceOptimizer;
	}

	public ReferenceRecyclingContext<A> getParentContext() {
		return parentContext;
	}

	public String getKeySuffix() {
		return keySuffix;
	}

	public String contextDescription() {
		return publicApiContext.toString();
	}

	public A publicApiContext() {
		return publicApiContext;
	}

	public ReferenceRecyclingContext<A> childContext(String keySuffix, Function<ReferenceRecyclingContext<A>, A> publicApiContextFactory) {
		if (sealed) {
			return new ReferenceRecyclingContext<>(this, keySuffix, publicApiContextFactory);
		}

		return knownChildContexts.computeIfAbsent(keySuffix, k -> new ReferenceRecyclingContext<>(this, keySuffix, publicApiContextFactory));
	}

	public ReferenceRecyclingContext<A> childContext(String key) {
		return childContext(key, publicApiContextFactory);
	}

	public void seal() {
		sealed = true;
		jsonReferenceOptimizer.seal();
	}
	
	public boolean isSealed() {
		return sealed;
	}
	
	@Override
	public String toString() {
		return (sealed ? "sealed " : "mutable ") + contextDescription();
	}
}
