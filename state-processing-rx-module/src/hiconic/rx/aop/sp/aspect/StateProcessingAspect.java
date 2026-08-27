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
package hiconic.rx.aop.sp.aspect;

import static com.braintribe.model.generic.manipulation.util.ManipulationBuilder.compound;
import static com.braintribe.model.generic.manipulation.util.ManipulationBuilder.entityProperty;
import static com.braintribe.utils.lcd.CollectionTools2.isEmpty;
import static com.braintribe.utils.lcd.CollectionTools2.newLinkedMap;
import static com.braintribe.utils.lcd.CollectionTools2.newList;
import static com.braintribe.utils.lcd.CollectionTools2.newMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.braintribe.cc.lcd.CodingMap;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.builder.vd.VdBuilder;
import com.braintribe.model.generic.commons.EntRefHashingComparator;
import com.braintribe.model.generic.manipulation.ChangeValueManipulation;
import com.braintribe.model.generic.manipulation.CompoundManipulation;
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.ManipulationType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.generic.value.PersistentEntityReference;
import com.braintribe.model.generic.value.PreliminaryEntityReference;
import com.braintribe.model.processing.aop.api.aspect.AccessAspect;
import com.braintribe.model.processing.aop.api.aspect.AccessJoinPoint;
import com.braintribe.model.processing.aop.api.aspect.PointCutConfigurationContext;
import com.braintribe.model.processing.aop.api.context.AroundContext;
import com.braintribe.model.processing.aop.api.interceptor.AroundInterceptor;
import com.braintribe.model.processing.aop.api.interceptor.InterceptionException;
import com.braintribe.model.processing.aop.api.service.AopIncrementalAccess;
import com.braintribe.model.processing.manipulation.basic.normalization.Normalizer;
import com.braintribe.model.processing.session.api.persistence.CommitListener;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

import hiconic.rx.aop.sp.api.HasAsyncExecutionPriority;
import hiconic.rx.aop.sp.api.StateChangeProcessor;
import hiconic.rx.aop.sp.api.StateChangeProcessorCapabilities;
import hiconic.rx.aop.sp.api.StateChangeProcessorMatch;
import hiconic.rx.aop.sp.api.StateChangeProcessorRule;
import hiconic.rx.aop.sp.api.StateChangeProcessorRuleSet;
import hiconic.rx.aop.sp.api.invocation.StateChangeProcessorInvocation;
import hiconic.rx.aop.sp.api.invocation.StateChangeProcessorInvocationPacket;
import hiconic.rx.aop.sp.commons.AfterStateChangeContextImpl;
import hiconic.rx.aop.sp.commons.BeforeStateChangeContextImpl;
import hiconic.rx.aop.sp.commons.StateChangeProcessorSelectorContextImpl;
import hiconic.rx.aop.sp.invocation.AbstractSpProcessing;

/**
 * An {@link AccessAspect} that is hooked into the {@link AopIncrementalAccess} to track changes in the persistence level.
 * <p>
 * It is configured with a list of {@link StateChangeProcessorRule}s that it will scan and, if matching, eventually call their
 * {@link StateChangeProcessor}.
 * 
 * @author pit
 * @author dirk
 */
public class StateProcessingAspect extends AbstractSpProcessing
		implements AccessAspect, AroundInterceptor<ManipulationRequest, ManipulationResponse> {

	private static Logger log = Logger.getLogger(StateProcessingAspect.class);

	private StateChangeProcessorRuleSet processorRuleSet;
	private Consumer<StateChangeProcessorInvocationPacket> asyncInvocationQueue;

	@Required
	public void setProcessorRuleSet(StateChangeProcessorRuleSet processorRuleSet) {
		this.processorRuleSet = processorRuleSet;
	}

	@Required
	public void setAsyncInvocationQueue(Consumer<StateChangeProcessorInvocationPacket> asyncInvocationQueue) {
		this.asyncInvocationQueue = asyncInvocationQueue;
	}

	@Override
	public void configurePointCuts(PointCutConfigurationContext context) {
		context.addPointCutBinding(AccessJoinPoint.applyManipulation, this);
	}

	/**
	 * class to replace entity property instances with preliminary references to their counterpart after the manipulations have been applied by the
	 * access maps {@link EntityReference} to {@link PersistentEntityReference}
	 * 
	 * @author pit
	 *
	 */
	private class ReferenceAdapter {
		private final Map<EntityReference, PersistentEntityReference> translatedReferenceMap = CodingMap.create(EntRefHashingComparator.INSTANCE);
		private final Map<EntityReference, EntityReference> inverseMap = CodingMap.create(EntRefHashingComparator.INSTANCE);

		private ReferenceAdapter(Manipulation manipulation) {
			manipulation.stream() //
					.filter(m -> m.manipulationType() == ManipulationType.CHANGE_VALUE) //
					.forEach(m -> visitCvm((ChangeValueManipulation) m));
		}

		private void visitCvm(ChangeValueManipulation cvm) {
			EntityProperty entityProperty = (EntityProperty) cvm.getOwner();
			EntityReference reference = entityProperty.getReference();

			if (reference instanceof PreliminaryEntityReference) {
				if (GenericEntity.id.equals(entityProperty.getPropertyName())) {
					PersistentEntityReference newRef = VdBuilder.persistentReference(reference.getTypeSignature(), cvm.getNewValue(), null);

					translatedReferenceMap.put(reference, newRef);
					inverseMap.put(newRef, reference);
				}

			} else if (GenericEntity.partition.equals(entityProperty.getPropertyName())) {
				EntityReference ref = inverseMap.remove(entityProperty.getReference());
				if (ref != null) {
					PersistentEntityReference translatedRef = translatedReferenceMap.get(ref);
					translatedRef.setRefPartition((String) cvm.getNewValue());
				}
			}
		}

		/**
		 * translates an entity property with a {@link PreliminaryEntityReference} into one with a proper {@link PersistentEntityReference}
		 * 
		 * @return - if needed a new {@link EntityProperty} or the one passed
		 */
		public EntityProperty adapt(EntityProperty entityProperty) {
			EntityReference reference = entityProperty.getReference();

			if (reference instanceof PreliminaryEntityReference) {
				PersistentEntityReference adaptedReference = translatedReferenceMap.get(reference);
				String propertyName = entityProperty.getPropertyName();
				entityProperty = entityProperty(adaptedReference, propertyName);
			}

			return entityProperty;
		}

		public EntityReference adapt(EntityReference entityReference) {
			if (entityReference instanceof PreliminaryEntityReference)
				entityReference = translatedReferenceMap.get(entityReference);

			return entityReference;
		}
	}

	/**
	 * helper class to bind the rule and the respective custom data.
	 * 
	 * @author pit
	 *
	 */
	private class ProcessorAndCustomData {
		public StateChangeProcessorRule processorRule;
		public StateChangeProcessorMatch processorMatch;
		public GenericEntity customData;
		public StateChangeProcessorCapabilities capabilities;
		public EntityType<?> entityType;

		public <T extends GenericEntity, C extends GenericEntity> StateChangeProcessor<T, C> getProcessor() {
			return (StateChangeProcessor<T, C>) processorMatch.getStateChangeProcessor();
		}
	}

	private class ProcessorMatches {
		public List<ProcessorAndCustomData> processors;
		public EntityProperty entityProperty;
		public EntityReference entityReference;
	}

	@Override
	public ManipulationResponse run(AroundContext<ManipulationRequest, ManipulationResponse> context) throws InterceptionException {
		ManipulationCollectingListener commitListener = null;

		try {
			ManipulationRequest manipulationRequest = context.getRequest();
			Manipulation manipulation = manipulationRequest.getManipulation();

			List<Manipulation> ms = Normalizer.normalize(manipulation).getCompoundManipulationList();

			// retrieve relevant state change processors
			Map<StateChangeProcessor<?, ?>, Object> processorContexts = newMap();
			Map<Manipulation, ProcessorMatches> manipulationToProcessorsMap = retrieveInvolvedStateChangeProcessors(context, ms, processorContexts);

			//
			// call before
			//
			for (Entry<Manipulation, ProcessorMatches> entry : manipulationToProcessorsMap.entrySet()) {
				Manipulation currentManipulation = entry.getKey();
				ProcessorMatches pms = entry.getValue();

				BeforeStateChangeContextImpl<GenericEntity> stateChangeContext = new BeforeStateChangeContextImpl<>(context.getSession(),
						context.getSystemSession(), pms.entityReference, pms.entityProperty, currentManipulation);

				// call the processors' onBefore method, synchronously
				for (ProcessorAndCustomData pair : pms.processors) {
					StateChangeProcessor<GenericEntity, GenericEntity> processor = pair.getProcessor();

					StateChangeProcessorCapabilities capabilities = processor.getCapabilities();
					pair.capabilities = capabilities;

					if (!capabilities.getBefore())
						continue;

					EntityType<GenericEntity> entityType = pair.entityType.cast();
					stateChangeContext.setEntityType(entityType);
					stateChangeContext.setProcessorContext(processorContexts.get(processor));
					stateChangeContext.initializeCapabilities(capabilities);
					// call before aspect of the processor and store the data it may return
					GenericEntity customContext = processor.onBeforeStateChange(stateChangeContext);

					stateChangeContext.commitIfNecessary();

					if (stateChangeContext.getCapabilitiesOverriden())
						pair.capabilities = stateChangeContext.getCapabilities();

					pair.customData = customContext;
				}
			}

			//
			//
			// actually execute manipulation
			//
			ManipulationResponse manipulationResponse = context.proceed();

			//
			// extract id translation map..
			//
			Manipulation allManipulation = merge(ms, manipulationResponse);

			ReferenceAdapter referenceAdapter = new ReferenceAdapter(allManipulation);

			List<Manipulation> collectedManipulations = newList();

			commitListener = new ManipulationCollectingListener(collectedManipulations);
			context.getSession().listeners().add(commitListener);

			//
			// call the after
			//
			for (Entry<Manipulation, ProcessorMatches> entry : manipulationToProcessorsMap.entrySet()) {
				Manipulation currentManipulation = entry.getKey();
				ProcessorMatches processorMatches = entry.getValue();
				EntityProperty entityProperty = processorMatches.entityProperty;
				EntityReference entityReference = processorMatches.entityReference;
				EntityReference adaptedEntityReference = referenceAdapter.adapt(entityReference);
				EntityProperty adaptedEntityProperty = entityProperty != null ? referenceAdapter.adapt(entityProperty) : null;

				//
				AfterStateChangeContextImpl<GenericEntity> afterStateChangeContext = new AfterStateChangeContextImpl<>( //
						context.getSession(), context.getSystemSession(), adaptedEntityReference, adaptedEntityProperty, currentManipulation);

				afterStateChangeContext.setReferenceMap(referenceAdapter.translatedReferenceMap);
				afterStateChangeContext.setInducedManipulations(collectedManipulations);

				InvocationPacketBuilder packetBuilder = new InvocationPacketBuilder();

				// call the processors
				for (ProcessorAndCustomData pair : entry.getValue().processors) {
					StateChangeProcessorRule rule = pair.processorRule;
					StateChangeProcessor<GenericEntity, GenericEntity> processor = pair.getProcessor();

					StateChangeProcessorCapabilities capabilities = pair.capabilities;

					// the optional after processing
					if (capabilities.getAfter()) {
						afterStateChangeContext.initializeCapabilities(capabilities);
						EntityType<GenericEntity> entityType = pair.entityType.cast();
						afterStateChangeContext.setEntityType(entityType);
						afterStateChangeContext.setProcessorContext(processorContexts.get(processor));

						// call the onAfter method synchronously
						processor.onAfterStateChange(afterStateChangeContext, pair.customData);

						afterStateChangeContext.commitIfNecessary();
					}

					if (afterStateChangeContext.getCapabilitiesOverriden())
						capabilities = afterStateChangeContext.getCapabilities();

					// the optional asynchronous processing
					if (capabilities.getProcess())
						packetBuilder.buildDeferredInvocation(rule.getRuleId(), pair.processorMatch, context.getSession().getAccessId(),
								adaptedEntityReference, adaptedEntityProperty, currentManipulation, pair.customData);

				}

				// enqueue async invocations
				if (packetBuilder.invocationPacket != null) {
					try {
						asyncInvocationQueue.accept(packetBuilder.invocationPacket);
					} catch (Exception e) {
						log.error("error while enqueing invocation packet with the following invocations: "
								+ packetBuilder.invocationPacket.getInvocations(), e);
					}
				}
			}

			return addInducedManipulations(manipulationResponse, collectedManipulations);

		} catch (Exception e) {
			throw new InterceptionException("error during applyManipulation as " + e, e);

		} finally {
			if (commitListener != null)
				context.getSession().listeners().remove(commitListener);
		}
	}

	private CompoundManipulation merge(List<Manipulation> ms, ManipulationResponse manipulationResponse) {
		CompoundManipulation result = compound();

		List<Manipulation> list = result.getCompoundManipulationList();
		list.addAll(ms);

		Manipulation im = manipulationResponse.getInducedManipulation();
		if (im != null)
			list.add(im);

		return result;
	}

	private static class InvocationPacketBuilder {
		StateChangeProcessorInvocationPacket invocationPacket;

		void ensurePacket() {
			if (invocationPacket == null)
				invocationPacket = StateChangeProcessorInvocationPacket.T.create();
		}

		public void buildDeferredInvocation(String ruleId, StateChangeProcessorMatch processorMatch, String accessId, EntityReference entityReference,
				EntityProperty entityProperty, Manipulation manipulation, GenericEntity customData) {

			ensurePacket();

			String processorId = processorMatch.getProcessorId();

			StateChangeProcessorInvocation invocation = StateChangeProcessorInvocation.T.create();
			invocation.setAccessId(accessId);
			invocation.setRuleId(ruleId);
			invocation.setProcessorId(processorId);
			invocation.setEntityProperty(entityProperty);
			invocation.setEntityReference(entityReference);
			invocation.setManipulation(manipulation);
			invocation.setCustomData(customData);

			if (processorMatch instanceof HasAsyncExecutionPriority) {
				HasAsyncExecutionPriority hasPriority = (HasAsyncExecutionPriority) processorMatch;
				double newPriority = Math.max(invocationPacket.getExecutionPriority(), hasPriority.getAsyncExecutionPriority());
				invocationPacket.setExecutionPriority(newPriority);
			}

			invocationPacket.getInvocations().add(invocation);
		}
	}

	private ManipulationResponse addInducedManipulations(ManipulationResponse manipulationResponse, List<Manipulation> manipulationsToAdd) {
		if (!isEmpty(manipulationsToAdd)) {
			Manipulation inducedManipulation = manipulationResponse.getInducedManipulation();
			CompoundManipulation compoundInducedManipulation = null;

			if (inducedManipulation == null) {
				compoundInducedManipulation = compound();
			} else {
				if (inducedManipulation.manipulationType() == ManipulationType.COMPOUND)
					compoundInducedManipulation = (CompoundManipulation) inducedManipulation;
				else
					compoundInducedManipulation = compound(inducedManipulation);
			}

			compoundInducedManipulation.getCompoundManipulationList().addAll(manipulationsToAdd);
			manipulationResponse.setInducedManipulation(compoundInducedManipulation);
		}

		return manipulationResponse;
	}

	/**
	 * retrieves a list of all involved StateChangeProcessors, grouped by the PropertyManipulation they're attached to
	 * 
	 * @param manipulations
	 *            - the collection of distinct property manipulations
	 * @param processorContexts
	 *            - this parameter is used for output - the map is expected to be empty and after the method is invoked, it map all the used
	 *            CustomContextProvidingProcessor to their respective contexts
	 * @return - a map that maps the property manipulation to the state change processors that are mapped to it
	 */
	private Map<Manipulation, ProcessorMatches> retrieveInvolvedStateChangeProcessors(
			AroundContext<ManipulationRequest, ManipulationResponse> aroundContext, Collection<Manipulation> manipulations,
			Map<StateChangeProcessor<?, ?>, Object> processorContexts) {

		Map<Manipulation, ProcessorMatches> result = newLinkedMap();
		Map<StateChangeProcessorRule, Object> customContexts = newMap();
		for (Manipulation manipulation : manipulations) {
			List<ProcessorAndCustomData> list = null;
			StateChangeProcessorSelectorContextImpl context = new StateChangeProcessorSelectorContextImpl(aroundContext.getSession(),
					aroundContext.getSystemSession(), manipulation);
			if (processorRuleSet.getProcessorRules() != null) {
				for (StateChangeProcessorRule rule : processorRuleSet.getProcessorRules()) {
					Object customContext = customContexts.get(rule);
					context.setCustomContext(customContext);

					List<StateChangeProcessorMatch> matches = rule.matches(context);

					customContexts.put(rule, context.getCustomContext());
					for (StateChangeProcessorMatch stateChangeProcessorMatch : matches) {
						if (list == null)
							list = newList();

						ProcessorAndCustomData customData = new ProcessorAndCustomData();
						customData.processorRule = rule;
						customData.entityType = context.getEntityType();
						customData.processorMatch = stateChangeProcessorMatch;
						list.add(customData);

						StateChangeProcessor<?, ?> scp = customData.getProcessor();

						processorContexts.computeIfAbsent(scp, k -> scp.createProcessorContext());
					}

					if (list != null) {
						ProcessorMatches processorMatches = new ProcessorMatches();
						processorMatches.processors = list;
						processorMatches.entityProperty = context.getEntityProperty();
						processorMatches.entityReference = context.getEntityReference();
						result.put(manipulation, processorMatches);
					}
				}
			}
		}

		return result;
	}

	class ManipulationCollectingListener implements CommitListener {
		private final List<Manipulation> manipulations;

		public ManipulationCollectingListener(List<Manipulation> manipulations) {
			this.manipulations = manipulations;
		}

		@Override
		public void onBeforeCommit(PersistenceGmSession session, Manipulation manipulation) {
			// NO OP
		}

		@Override
		public void onAfterCommit(PersistenceGmSession session, Manipulation manipulation, Manipulation inducedManipluation) {
			if (manipulation != null)
				manipulations.add(manipulation);

			if (inducedManipluation != null)
				manipulations.add(inducedManipluation);
		}
	}

}
