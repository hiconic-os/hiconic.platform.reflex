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

import hiconic.rx.openapi.v3.processing.processor.reference.JsonReferenceOptimizer.OptimizationResult;

/**
 * This exception is just to signal that a Reference can't be created because it is not valid in the current
 * {@link ReferenceRecyclingContext}. This means that eventual referencing parents can't be created as well and the
 * current evaluation of the reference tree should be aborted.
 * <p>
 * This is not a classical Exception and should never be reported to the user. Note also that no stack trace is created.
 *
 * @see ReferenceRecycler#isValidInContext(ReferenceRecyclingContext)
 * @author Neidhart.Orlich
 *
 */
public class CantBuildReferenceException extends RuntimeException {
	private static final long serialVersionUID = 1997812406963071558L;
	@SuppressWarnings("unused") // useful for debugging
	private final ReferenceRecyclingContext<?> context;
	@SuppressWarnings("unused") // useful for debugging
	private final ReferenceRecycler<?, ?> referenceRecycler;
	@SuppressWarnings("unused") // useful for debugging
	private final OptimizationResult optimizationResult;

	public CantBuildReferenceException(ReferenceRecyclingContext<?> context, ReferenceRecycler<?, ?> referenceRecycler,
			OptimizationResult optimizationResult) {
		super("Can't provide component '" + referenceRecycler.getContextUnawareRefString() + "' for context " + context.contextDescription()
				+ " nor any child and parent contexts.", null, false, false);
		this.context = context;
		this.referenceRecycler = referenceRecycler;
		this.optimizationResult = optimizationResult;
	}

}
