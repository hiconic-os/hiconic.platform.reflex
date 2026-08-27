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
package hiconic.rx.aop.idgen.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.braintribe.cc.lcd.CodingMap;
import com.braintribe.model.access.IncrementalAccess;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.commons.EntRefHashingComparator;
import com.braintribe.model.generic.manipulation.ChangeValueManipulation;
import com.braintribe.model.generic.manipulation.CompoundManipulation;
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.InstantiationManipulation;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.NormalizedCompoundManipulation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.GenericModelTypeReflection;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.generic.value.PersistentEntityReference;
import com.braintribe.model.generic.value.PreliminaryEntityReference;
import com.braintribe.model.processing.aop.api.aspect.AccessAspect;
import com.braintribe.model.processing.aop.api.aspect.AccessAspectRuntimeException;
import com.braintribe.model.processing.aop.api.aspect.AccessJoinPoint;
import com.braintribe.model.processing.aop.api.aspect.PointCutConfigurationContext;
import com.braintribe.model.processing.aop.api.context.AroundContext;
import com.braintribe.model.processing.aop.api.interceptor.AroundInterceptor;
import com.braintribe.model.processing.aop.api.interceptor.InterceptionException;

/**
 * This {@link AccessAspect} ensures that entities receive a global unique id by using {@link UUID#randomUUID()}. Therefore the aspect wraps around
 * {@link IncrementalAccess#applyManipulation(ManipulationRequest)} to scan manipulation requests. Automatic global id assignment manipulations are
 * only appended if not already done explicitly. If they are appended then they are also prepended before the induced manipulations in order to inform
 * any client about the assigned global ids.
 * 
 * The algorithm also keeps track of persistent id assignment to be consistent with its trackings.
 *
 * @author dirk.scheffler
 */
public class GlobalIdGeneratorAspect implements AccessAspect, AroundInterceptor<ManipulationRequest, ManipulationResponse> {

	@Override
	public void configurePointCuts(PointCutConfigurationContext context) throws AccessAspectRuntimeException {
		context.addPointCutBinding(AccessJoinPoint.applyManipulation, this);
	}

	@Override
	public ManipulationResponse run(AroundContext<ManipulationRequest, ManipulationResponse> context) throws InterceptionException {
		try {
			ManipulationRequest request = context.getRequest();

			Manipulation manipulation = request.getManipulation();

			if (manipulation != null) {
				IdAssignmentAnalytics analytics = new IdAssignmentAnalytics();

				Manipulation globalIdAssignments = analytics.generateGlobalIdAssignments(manipulation);

				if (globalIdAssignments != null) {
					Manipulation extendedManipulation = extendManipulations(manipulation, globalIdAssignments, false);
					request.setManipulation(extendedManipulation);
					context.overrideRequest(request);
					ManipulationResponse response = context.proceed();

					Manipulation inducedManipulation = response.getInducedManipulation();
					Manipulation prependedInducedManipulation = extendManipulations(inducedManipulation, globalIdAssignments, true);
					response.setInducedManipulation(prependedInducedManipulation);

					return response;
				}
			}

			return context.proceed();

		} catch (Exception e) {
			throw new InterceptionException("error while processing manipulations in GlobalIdGeneratorAspect", e);
		}
	}

	private Manipulation extendManipulations(Manipulation originalManipulation, Manipulation extendManipulation, boolean prepend) {
		List<Manipulation> originalManipulations = manipulationAsList(originalManipulation);
		List<Manipulation> extendManipulations = manipulationAsList(extendManipulation);
		List<Manipulation> extendedManipulations = new ArrayList<>(originalManipulations.size() + extendManipulations.size());

		if (prepend) {
			extendedManipulations.addAll(extendManipulations);
			extendedManipulations.addAll(originalManipulations);
		} else {
			extendedManipulations.addAll(originalManipulations);
			extendedManipulations.addAll(extendManipulations);
		}

		switch (extendedManipulations.size()) {
			case 0:
				return null;
			case 1:
				return extendedManipulations.get(0);
			default:
				EntityType<? extends CompoundManipulation> resultType = originalManipulation instanceof NormalizedCompoundManipulation
						? NormalizedCompoundManipulation.T : CompoundManipulation.T;
				CompoundManipulation compoundManipulation = resultType.create();
				compoundManipulation.setCompoundManipulationList(extendedManipulations);
				return compoundManipulation;
		}
	}

	private List<Manipulation> manipulationAsList(Manipulation extendManipulation) {
		List<Manipulation> extendManipulations = null;

		if (extendManipulation == null) {
			extendManipulations = Collections.emptyList();
		} else if (extendManipulation instanceof CompoundManipulation) {
			extendManipulations = ((CompoundManipulation) extendManipulation).getCompoundManipulationList();

			if (extendManipulations == null) {
				extendManipulations = Collections.emptyList();
			}
		} else {
			extendManipulations = Collections.singletonList(extendManipulation);
		}
		return extendManipulations;
	}

	private static class IdAssignmentAnalytics {
		private final GenericModelTypeReflection typeReflection = GMF.getTypeReflection();
		private final EntityType<GenericEntity> hasGlobalIdType = typeReflection.getEntityType(GenericEntity.class);

		private final Map<EntityReference, GlobalIdAssignmentInfo> globalIdAssignmentInfoByReference = CodingMap
				.create(EntRefHashingComparator.INSTANCE);
		private final List<GlobalIdAssignmentInfo> globalIdAssignmentInfos = new ArrayList<>();

		public Manipulation generateGlobalIdAssignments(Manipulation manipulation) {
			scan(manipulation);

			List<Manipulation> idAssignments = new ArrayList<>(globalIdAssignmentInfos.size());

			for (GlobalIdAssignmentInfo info : globalIdAssignmentInfos) {
				if (info.lastGlobalId == null) {
					String globalId = UUID.randomUUID().toString();

					EntityProperty entityProperty = EntityProperty.T.create();
					entityProperty.setPropertyName(GenericEntity.globalId);
					entityProperty.setReference(info.lastReference);

					ChangeValueManipulation globalIdAssignment = ChangeValueManipulation.T.create();
					globalIdAssignment.setOwner(entityProperty);
					globalIdAssignment.setNewValue(globalId);
					idAssignments.add(globalIdAssignment);
				}
			}

			switch (idAssignments.size()) {
				case 0:
					return null;
				case 1:
					return idAssignments.get(0);
				default:
					CompoundManipulation compoundManipulation = CompoundManipulation.T.create();
					compoundManipulation.setCompoundManipulationList(idAssignments);
					return compoundManipulation;
			}
		}

		private void scan(Manipulation manipulation) {
			if (manipulation instanceof CompoundManipulation) {
				CompoundManipulation compoundManipulation = (CompoundManipulation) manipulation;
				List<Manipulation> manipulations = compoundManipulation.getCompoundManipulationList();
				if (manipulations != null) {
					for (Manipulation nestedManipulation : manipulations) {
						scan(nestedManipulation);
					}
				}
			} else {
				PreliminaryEntityReference instantiationReference = null;
				if ((instantiationReference = isInstantiationWithGlobalId(manipulation)) != null) {
					GlobalIdAssignmentInfo info = new GlobalIdAssignmentInfo();
					globalIdAssignmentInfos.add(info);
					info.lastReference = instantiationReference;
					globalIdAssignmentInfoByReference.put(instantiationReference, info);
				} else
					trackIdChanges(manipulation);
			}
		}

		private void trackIdChanges(Manipulation manipulation) {
			if (manipulation instanceof ChangeValueManipulation) {
				ChangeValueManipulation changeValueManipulation = (ChangeValueManipulation) manipulation;
				EntityProperty entityProperty = (EntityProperty) changeValueManipulation.getOwner();

				EntityReference reference = entityProperty.getReference();
				String propertyName = entityProperty.getPropertyName();

				GlobalIdAssignmentInfo info = globalIdAssignmentInfoByReference.get(reference);

				if (info != null) {
					// check for globalId change
					if (GenericEntity.globalId.equals(propertyName)) {
						String globalId = (String) changeValueManipulation.getNewValue();
						info.lastGlobalId = globalId;
					} else {
						// check for id change
						if (GenericEntity.id.equals(propertyName)) {
							// propagate info to new reference in order to keep track of it
							Object id = changeValueManipulation.getNewValue();

							if (id != null) {
								PersistentEntityReference persistentReference = PersistentEntityReference.T.create();
								persistentReference.setRefId(id);
								persistentReference.setTypeSignature(reference.getTypeSignature());
								info.lastReference = persistentReference;
								globalIdAssignmentInfoByReference.put(persistentReference, info);
							}
						}
					}
				}
			}
		}

		private PreliminaryEntityReference isInstantiationWithGlobalId(Manipulation manipulation) {
			if (manipulation instanceof InstantiationManipulation) {
				PreliminaryEntityReference reference = (PreliminaryEntityReference) ((InstantiationManipulation) manipulation).getEntity();
				if (isAssignableToHasGlobalId(reference.getTypeSignature()))
					return reference;
			}
			return null;
		}

		private boolean isAssignableToHasGlobalId(String typeSignature) {
			GenericModelType type = GMF.getTypeReflection().getType(typeSignature);
			return hasGlobalIdType.isAssignableFrom(type);
		}

	}

	/**
	 * this info holds information about the last EntityReference that resulted from persistent id assignments and about the last global id assignment
	 * which is needed to detect if an automatic assignment is needed
	 * 
	 * @author dirk.scheffler
	 *
	 */
	private static class GlobalIdAssignmentInfo {
		public EntityReference lastReference;
		public String lastGlobalId;
	}
}
