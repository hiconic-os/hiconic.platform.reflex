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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.braintribe.model.openapi.v3_0.JsonReferencable;

/**
 * Implementation of the actual optimization logic for the {@link ReferenceRecycler}
 * 
 * @see ReferenceRecycler
 * @author Neidhart.Orlich
 */
public class JsonReferenceOptimizer<A> implements OptimizationResults {

	private final Map<JsonReferencable, ProcessingCacheEntry<?>> cachedReferencesByRefInstance = new HashMap<>();
	private final Map<String, ProcessingCacheEntry<?>> cachedReferencesByRefString = new HashMap<>();
	private final Deque<ProcessingCacheEntry<?>> currentResolvingStack = new ArrayDeque<>();
	private final Deque<ProcessingCacheEntry<?>> cycleStartingPointsStack = new ArrayDeque<>();

	private final ReferenceRecyclingContext<A> context;

	private boolean sealed = false;

	public JsonReferenceOptimizer(ReferenceRecyclingContext<A> context) {
		this.context = context;
	}

	public enum ProcessingStatus {
		changed, // the current context resolves the component DIFFENRENTLY than the parent context
		unchanged, // the current context resolves the component SAME AS the parent context
		cycleDetected, // the components which are currently being resolved form a cycle.
		firstDetection, // the component has just been encountered the first time
		notPresent; // the component can't exist in this context i.e. because it's not part of its model

		public boolean statusKnown() {
			return this == changed || this == unchanged;
		}
	}

	/**
	 * A simplified facade interface for {@link JsonReferenceOptimizer.ProcessingCacheEntry} meant for outside use
	 *
	 * @author Neidhart.Orlich
	 *
	 */
	public interface OptimizationResult {
		String getRefString();
		JsonReferencable getReference();

		ProcessingStatus getStatus();
	}

	// Not a static class only because I want to reuse the 'A' type parameter
	private class ProcessingCacheEntry<T extends JsonReferencable> implements OptimizationResult {
		private final T reference; // This is just a reference to the resolvedComponent in the pool
		private T resolvedComponent; // This is the actual component that can be used for comparison with child
										// contexts.
										// in 'cycleDetected' entries this is also used to store the component from
										// the ancestor as long as it is not known if it was changed.
		private T currentContextComponent; // This saves the new component created by the current context in case it's
											// needed later
		private ReferenceRecyclingContext<A> resolvedComponentContext; // The current context when 'changed'; The
																		// (transitive) parent context when 'unchanged';
																		// null otherwise
		private final ReferenceRecycler<T, A> referenceRecycler;
		private ProcessingStatus status;

		ProcessingCacheEntry(ReferenceRecycler<T, A> referenceRecycler, T reference) {
			this.referenceRecycler = referenceRecycler;
			this.reference = reference;
		}

		ProcessingCacheEntry(ReferenceRecycler<T, A> referenceRecycler) {
			this.referenceRecycler = referenceRecycler;
			this.reference = referenceRecycler.getRawRef();
			status = ProcessingStatus.firstDetection;
		}

		@Override
		public String getRefString() {
			return reference.get$ref();
		}

		@Override
		public ProcessingStatus getStatus() {
			return status;
		}

		@Override
		public JsonReferencable getReference() {
			return reference;
		}

		@Override
		public String toString() {
			String contextDescription = resolvedComponentContext == null ? "<unresolved>" : resolvedComponentContext.contextDescription();
			return status + referenceRecycler.getContextUnawareRefString() + " of " + contextDescription;
		}
	}

	private <T extends JsonReferencable> ProcessingCacheEntry<T> notExistingInContext(ReferenceRecycler<T, A> component,
			ReferenceRecyclingContext<A> context) {
		ProcessingCacheEntry<T> cacheEntry = new ProcessingCacheEntry<>(component, null);
		cacheEntry.resolvedComponentContext = context;
		cacheEntry.status = ProcessingStatus.notPresent;

		return cacheEntry;
	}

	@SuppressWarnings("unused")
	private static void traceLog(String s) {
		// Enable for local debugging:
//		 System.out.println(s);
	}

	private <T extends JsonReferencable> ProcessingCacheEntry<T> resolveAsNotPresent(ReferenceRecycler<T, A> referenceRecycler) {
		String cacheKey = referenceRecycler.getContextUnawareRefString();

		// If the current component is not instantiable, the whole resolving stack isn't
		// A not-present component will result in a CantBuildReferenceException which is passed all the way to the end of the resolving stack 
		
		traceLog(" - '" + cacheKey + "' is not valid in context " + context.getKeySuffix());
		ProcessingCacheEntry<T> cacheEntry = notExistingInContext(referenceRecycler, context);
		cachedReferencesByRefString.put(cacheKey, cacheEntry);
		
		while (!currentResolvingStack.isEmpty()) {
			currentResolvingStack.pop().status = ProcessingStatus.notPresent;
		}
		cycleStartingPointsStack.clear();

		return cacheEntry;
	}

	private <T extends JsonReferencable> void resolveAsChanged(ProcessingCacheEntry<T> entry, T newComponent) {
		resolveAs(entry, ProcessingStatus.changed, newComponent, context);

		entry.reference.set$ref(entry.referenceRecycler.getContextAwareRefString(context));
	}

	private <T extends JsonReferencable> void resolveAsUnchanged(ProcessingCacheEntry<T> entry, ProcessingCacheEntry<T> ancestorEntry) {
		resolveAs(entry, ProcessingStatus.unchanged, ancestorEntry.resolvedComponent, ancestorEntry.resolvedComponentContext);

		entry.reference.set$ref(ancestorEntry.getRefString());
	}

	private <T extends JsonReferencable> void resolveEntryFromCycle(ProcessingCacheEntry<T> entry, ProcessingStatus status) {
		entry.status = status; // remove cycleDetected status

		traceLog("- resolving cycle entry " + entry.referenceRecycler.getContextUnawareRefString());
		
		if (cycleStartingPointsStack.contains(entry)) {
			traceLog("- " + entry + " is already starting point of another cycle.");
		}

		if (status == ProcessingStatus.unchanged) {
			resolveAsUnchanged(entry, entry);
		} else {
			resolveAsChanged(entry, entry.currentContextComponent);
		}
	}

	private <T extends JsonReferencable> void resolveAs(ProcessingCacheEntry<T> entry, ProcessingStatus status, T ancestorComponent,
			ReferenceRecyclingContext<A> ancestorContext) {
		if (!status.statusKnown()) {
			throw new RuntimeException("Error in Algorithm - component can't be resolved with unknown status");
		}

		if (ancestorComponent == null) {
			throw new RuntimeException("Error in Algorithm - unexpected null argument 'ancestorComponent'");
		}

		traceLog("- resolve as " + status + " " + entry.referenceRecycler.getContextUnawareRefString() + " [" + context.getKeySuffix() + " => "
				+ ancestorContext.getKeySuffix() + "]");

		if (status == ProcessingStatus.changed) {
			ReferenceRecycler<T, A> referenceRecycler = entry.referenceRecycler;
			referenceRecycler.storeInPool(ancestorContext, ancestorComponent);
		}

		entry.resolvedComponent = ancestorComponent;
		entry.resolvedComponentContext = ancestorContext;

		if (entry.status == ProcessingStatus.cycleDetected) {
			attemptResolveCycles(entry, status, ancestorComponent, ancestorContext);
		} else {
			ProcessingCacheEntry<?> poppedEntry = currentResolvingStack.pop();
			if (poppedEntry != entry) {
				throw new IllegalStateException("Error in Algorithm: Unexpected entry [" + poppedEntry +"] on top of currentResolvingStack. Expected [" + entry + "].");
			}
			entry.resolvedComponent = ancestorComponent;
			entry.resolvedComponentContext = ancestorContext;
			entry.status = status;

			traceLog("- resolved as " + status + " " + entry.referenceRecycler.getContextUnawareRefString());
		}
	}

	private <T extends JsonReferencable> void attemptResolveCycles(ProcessingCacheEntry<T> entry, ProcessingStatus status, T ancestorComponent,
			ReferenceRecyclingContext<A> ancestorContext) {
		if (cycleStartingPointsStack.isEmpty()) {
			throw new IllegalStateException("Error in algorithm: resolving cycle element but no cycleStartingPoint left in stack.");
		}
		// In a cycle the following needs to be respected:
		// If there are changes we don't need to check further - the whole cycle needs new components
		// If there aren't any changes still we need to check the rest of the cycle so we leave the 'cycleDetected'
		// status
		ProcessingCacheEntry<?> topmostCycleStart = cycleStartingPointsStack.peek();
		if (entry == topmostCycleStart) {
			traceLog("- ACTUAL RESOLVE BECAUSE we are at cycle start");
			// There could be several circles but at least the topmost one is resolved.
			// Set all stack entries that were not popped before because of their cycleDetected status
			cycleStartingPointsStack.pop();
			entry.resolvedComponent = ancestorComponent;
			entry.resolvedComponentContext = ancestorContext;
			resolveTopmostCycleSection(entry, status);

			if (cycleStartingPointsStack.isEmpty() && !currentResolvingStack.isEmpty()
					&& currentResolvingStack.peek().status == ProcessingStatus.cycleDetected) {
				throw new RuntimeException("Error in Algorithm: There is a cycle left but no starting point");
			}
		} else {
			if (cycleStartingPointsStack.contains(entry)) {
				traceLog("Entry " + entry + " is already cycle starting point but not the topmost one");
			}
			if (status == ProcessingStatus.unchanged) {
				traceLog("- NO ACTUAL RESOLVE BECAUSE WE ARE IN CYCLE with root [" + cycleStartingPointsStack.peek() + "].");
				// We set the entry as if it was unchanged to not lose the data if we need it later
				// It could still be changed however.
				
				entry.resolvedComponent = ancestorComponent;
				entry.resolvedComponentContext = ancestorContext;
			} else {
				traceLog("- ACTUAL RESOLVE BECAUSE change was detected");
				resolveTopmostCycleSection(entry, status);
			}
		}
	}

	private void resolveTopmostCycleSection(ProcessingCacheEntry<?> entry, ProcessingStatus status) {
		for (ProcessingCacheEntry<?> stackEntry = currentResolvingStack.peek(); stackEntry != entry; stackEntry = currentResolvingStack.peek()) {
			resolveEntryFromCycle(stackEntry, status);
		}

		resolveEntryFromCycle(entry, status);
	}

	private <T extends JsonReferencable> ProcessingCacheEntry<T> getOptimized__toBeCalledFromChildContext(Function<A, T> factory,
			ReferenceRecycler<T, A> referenceRecycler) {
		try {
			return getOptimized__impl(factory, referenceRecycler);
		} catch (CantBuildReferenceException e) {
			return resolveAsNotPresent(referenceRecycler);
		}
	}

	public <T extends JsonReferencable> ProcessingCacheEntry<T> getOptimized(Function<A, T> factory, ReferenceRecycler<T, A> referenceRecycler) {
		ProcessingCacheEntry<T> cacheEntry;
		try {
			cacheEntry = getOptimized__impl(factory, referenceRecycler);
		} catch (CantBuildReferenceException e) {
			cacheEntry = resolveAsNotPresent(referenceRecycler);

			throw e;
		}

		if (cacheEntry.status == ProcessingStatus.notPresent) {
			throw new CantBuildReferenceException(context, referenceRecycler, cacheEntry);
		}

		return cacheEntry;
	}

	private <T extends JsonReferencable> ProcessingCacheEntry<T> getOptimized__impl(Function<A, T> factory,
			ReferenceRecycler<T, A> referenceRecycler) {
		String cacheKey = referenceRecycler.getContextUnawareRefString();

		traceLog("Optimizing '" + cacheKey + "' - current context: " + context.getKeySuffix());

		ProcessingCacheEntry<T> cacheEntry = (ProcessingCacheEntry<T>) cachedReferencesByRefString.get(cacheKey);

		if (cacheEntry != null) {
			return handleCachedEntry(cacheKey, cacheEntry);
		}

		if (sealed) {
			traceLog(" - Tried to create '" + cacheKey + "' but context is already sealed: " + context.getKeySuffix());
		} else if (!referenceRecycler.isValidInContext(context)) {

			traceLog("Could not create component " + cacheKey + " because it's not valid in this context: " + context.contextDescription() + ".");

		} else {
			return handleNewEntry(factory, referenceRecycler);
		}

		// This context can't provide a valid component so try parent context

		ReferenceRecyclingContext<A> parentContext = context.getParentContext();
		if (parentContext != null) {
			JsonReferenceOptimizer<A> parentRefOptimizer = parentContext.getOptimizationResults();
			ProcessingCacheEntry<T> parentEntry;

			parentEntry = parentRefOptimizer.getOptimized__toBeCalledFromChildContext(factory, referenceRecycler);

			if (parentEntry.status.statusKnown()) {
				cacheEntry = parentEntry;
				cachedReferencesByRefString.put(cacheKey, cacheEntry);
				cachedReferencesByRefInstance.put(cacheEntry.reference, cacheEntry);
				return cacheEntry;
			}
		}

		return resolveAsNotPresent(referenceRecycler);
	}

	private <T extends JsonReferencable> ProcessingCacheEntry<T> handleNewEntry(Function<A, T> factory, ReferenceRecycler<T, A> referenceRecycler) {

		traceLog("- New entry" + referenceRecycler.getContextUnawareRefString());

		String cacheKey = referenceRecycler.getContextUnawareRefString();
		ProcessingCacheEntry<T> cacheEntry = new ProcessingCacheEntry<>(referenceRecycler);

		cachedReferencesByRefString.put(cacheKey, cacheEntry);
		cachedReferencesByRefInstance.put(cacheEntry.reference, cacheEntry);

		currentResolvingStack.push(cacheEntry);

		ReferenceRecyclingContext<A> parentContext = context.getParentContext();
		if (parentContext != null) {
			JsonReferenceOptimizer<A> parentRefOptimizer = parentContext.getOptimizationResults();
			ProcessingCacheEntry<T> parentResult = (ProcessingCacheEntry<T>) parentRefOptimizer.getCached(cacheKey);

			if (parentResult == null) {
				traceLog("- create with parent context first " + cacheKey);
				parentResult = parentRefOptimizer.getOptimized__toBeCalledFromChildContext(factory, referenceRecycler);
			}
			
			// the factory for the current context must run after all parent contexts
			cacheEntry.currentContextComponent = factory.apply(context.publicApiContext());
			JsonReferenceComparator refComparator = new JsonReferenceComparator(this::getStatusFor);

			if (parentResult.status != ProcessingStatus.notPresent) {
				if (!parentResult.status.statusKnown()) {
					throw new RuntimeException("Error in Algorithm: cached parent result is not 'changed' or 'unchanged' but " + parentResult.status);
				}

				JsonReferencable createdWithParent = parentResult.resolvedComponent;

				if (refComparator.preferAncestor(createdWithParent, cacheEntry.currentContextComponent)) {
					resolveAsUnchanged(cacheEntry, parentResult);
					return cacheEntry;
				}
			}
		} else {
			cacheEntry.currentContextComponent = factory.apply(context.publicApiContext());
		}

		// At this point the current context provides the optimized result which has not been registered in components
		// OR cached before

		resolveAsChanged(cacheEntry, cacheEntry.currentContextComponent);

		return cacheEntry;
	}

	private ProcessingStatus getStatusFor(JsonReferencable entity) {
		ProcessingCacheEntry<?> cached = getCached(entity);
		if (cached == null) {
			return null;
		}

		return cached.getStatus();
	}

	private <T extends JsonReferencable> ProcessingCacheEntry<T> handleCachedEntry(String cacheKey, ProcessingCacheEntry<T> cacheEntry) {
		// TODO: In case of a self-reference we might not need to declare a cycle. However a new ProcessingStatus would
		// have to be
		// introduced for that special case. Not sure if that improves the performance enough to complicate the logic.
		
		boolean firstDetection = false;
		
		if (cacheEntry.status == ProcessingStatus.firstDetection) {
			// cycle detected!
			cacheEntry.status = ProcessingStatus.cycleDetected;
			firstDetection = true;
		}

		// If the result was not cached we are back at the start of the cycle and need to resolve the result now.
		// Otherwise we return the cache entry that later has to be resolved.
		if (cacheEntry.status == ProcessingStatus.cycleDetected) {
			traceLog("- cycle " + cacheKey);
			
			ProcessingCacheEntry<?> currentCycleStartingPoint = cycleStartingPointsStack.peek();
			
			for (ProcessingCacheEntry<?> e : currentResolvingStack) {
				if (e == cacheEntry) {
					if (firstDetection) {
						cycleStartingPointsStack.push(cacheEntry);
					}
					break; // we are at the start of the topmost cycle. If there was a cycle below it must have already
					// been found and declared
				}
				
				if (e == currentCycleStartingPoint) {
					cycleStartingPointsStack.pop();
					currentCycleStartingPoint = cycleStartingPointsStack.peek();
				}
				
				e.status = ProcessingStatus.cycleDetected;
			}
			
			return cacheEntry;
		}

		if (cacheEntry.status.statusKnown() || cacheEntry.status == ProcessingStatus.notPresent) {
			// this context already has a cached and known result
			traceLog("- cached " + cacheKey);
			return cacheEntry;
		}

		throw new IllegalStateException("Cached entry has unexpected status " + cacheEntry.status);
	}

	@Override
	public ProcessingCacheEntry<?> getCached(String cacheKey) {
		return cachedReferencesByRefString.get(cacheKey);
	}

	@Override
	public ProcessingCacheEntry<?> getCached(JsonReferencable ref) {
		Objects.nonNull(ref);
		return cachedReferencesByRefInstance.get(ref);
	}

	private void assertFinished() {
		if (!currentResolvingStack.isEmpty()) {
			throw new RuntimeException("Error in Algorithm: context is expected to have finished optimizations but resolving stack isn't empty.");
		}

		if (!cycleStartingPointsStack.isEmpty()) {
			throw new RuntimeException(
					"Error in Algorithm: context is expected to have finished optimizations but cycle starting points isn't empty.");
		}
	}

	@Override
	public void seal() {
		assertFinished();

		sealed = true;
	}

}
