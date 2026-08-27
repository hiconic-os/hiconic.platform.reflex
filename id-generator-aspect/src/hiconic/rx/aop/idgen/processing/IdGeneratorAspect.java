// ============================================================================
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.braintribe.cc.lcd.CodingMap;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.commons.EntRefHashingComparator;
import com.braintribe.model.generic.manipulation.AtomicManipulation;
import com.braintribe.model.generic.manipulation.ChangeValueManipulation;
import com.braintribe.model.generic.manipulation.CompoundManipulation;
import com.braintribe.model.generic.manipulation.EntityProperty;
import com.braintribe.model.generic.manipulation.InstantiationManipulation;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.Owner;
import com.braintribe.model.generic.pr.criteria.EntityCriterion;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityVisitor;
import com.braintribe.model.generic.reflection.GenericModelTypeReflection;
import com.braintribe.model.generic.reflection.TraversingContext;
import com.braintribe.model.generic.value.EntityReference;
import com.braintribe.model.processing.aop.api.aspect.AccessAspect;
import com.braintribe.model.processing.aop.api.aspect.AccessJoinPoint;
import com.braintribe.model.processing.aop.api.aspect.PointCutConfigurationContext;
import com.braintribe.model.processing.aop.api.context.AroundContext;
import com.braintribe.model.processing.aop.api.interceptor.AroundInterceptor;
import com.braintribe.model.processing.aop.api.interceptor.InterceptionException;
import com.braintribe.model.processing.idgenerator.api.BasicIdGeneratorContext;
import com.braintribe.model.processing.idgenerator.api.IdGenerator;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

import hiconic.rx.aop.idgen.model.meta.GenerateIdWith;

/**
 * Analyzes incoming Manipulations and generates a new id for every {@link InstantiationManipulation instantiation} of a type with
 * {@link GenerateIdWith} Md. The id is assigned by adding a {@link ChangeValueManipulation} is created for the id property.
 * 
 * @author gunther.schenk
 */
public class IdGeneratorAspect implements AccessAspect {

	private final GenericModelTypeReflection typeReflection = GMF.getTypeReflection();

	@Override
	public void configurePointCuts(PointCutConfigurationContext context) {
		context.addPointCutBinding(AccessJoinPoint.applyManipulation, new IdGeneratorInterceptor());

	}

	private class IdGeneratorInterceptor implements AroundInterceptor<ManipulationRequest, ManipulationResponse> {

		@Override
		public ManipulationResponse run(final AroundContext<ManipulationRequest, ManipulationResponse> context) throws InterceptionException {

			final PersistenceGmSession session = context.getSession();
			final ManipulationRequest request = context.getRequest();
			final Manipulation manipulation = request.getManipulation();

			final Map<EntityReference, ChangeValueManipulation> idManipulations = CodingMap.create(EntRefHashingComparator.INSTANCE);
			final Map<EntityReference, InstantiationManipulation> instManis = CodingMap.create(EntRefHashingComparator.INSTANCE);

			// First collect all Instantiations and Id Property assignments
			EntityType<Manipulation> manipulationType = manipulation.entityType();
			manipulationType.traverse(manipulation, null, new EntityVisitor() {

				@Override
				protected void visitEntity(GenericEntity entity, EntityCriterion criterion, TraversingContext traversingContext) {
					if (entity instanceof ChangeValueManipulation) {
						ChangeValueManipulation changeValueManipulation = (ChangeValueManipulation) entity;
						EntityProperty entityProperty = getEntityProperty(changeValueManipulation);

						if (entityProperty != null)
							if (isIdPropertyManipulation(entityProperty))
								idManipulations.put(entityProperty.getReference(), changeValueManipulation);
					}

					if (entity instanceof InstantiationManipulation) {
						InstantiationManipulation im = (InstantiationManipulation) entity;
						EntityReference ref = getEntityReference(im);
						instManis.put(ref, im);
					}
				}
			});

			final Map<String, ChangeValueManipulation> additionalIdManis = collectAdditionalManis(context, session, idManipulations, instManis);

			if (!additionalIdManis.isEmpty()) {

				List<AtomicManipulation> manis = request.getManipulation().inline();
				List<Manipulation> newManipulations = new ArrayList<>();
				for (AtomicManipulation mani : manis) {
					newManipulations.add(mani);
					if (mani instanceof InstantiationManipulation) {
						EntityReference entityReference = getEntityReference((InstantiationManipulation) mani);
						ChangeValueManipulation additionalIdManipulation = findAdditionalIdManipulation(additionalIdManis, entityReference);
						if (additionalIdManipulation != null) {
							newManipulations.add(additionalIdManipulation);
						}
					}
				}
				CompoundManipulation compoundRequestManipulation = combineManipulations(null, newManipulations);
				request.setManipulation(compoundRequestManipulation);
			}

			try {
				ManipulationResponse response = context.proceed(request);
				if (!additionalIdManis.isEmpty()) {
					CompoundManipulation indecedMani = CompoundManipulation.T.create();
					indecedMani.setCompoundManipulationList(new ArrayList<Manipulation>());
					indecedMani.getCompoundManipulationList().addAll(additionalIdManis.values());
					if (response.getInducedManipulation() != null) {
						indecedMani.getCompoundManipulationList().add(response.getInducedManipulation());
					}
					response.setInducedManipulation(indecedMani);
				}
				return response;
			} catch (Exception e) {
				throw new InterceptionException("An error occurred while generating ids.", e);
			}

		}

		private ChangeValueManipulation findAdditionalIdManipulation(final Map<String, ChangeValueManipulation> additionalIdManipulations,
				EntityReference entityReference) {

			String key = "".concat(entityReference.getTypeSignature()).concat("#").concat(entityReference.getId().toString());
			return additionalIdManipulations.get(key);
		}

		private Map<String, ChangeValueManipulation> collectAdditionalManis(final AroundContext<ManipulationRequest, ManipulationResponse> context,
				final PersistenceGmSession session, final Map<EntityReference, ChangeValueManipulation> idManipulations,
				final Map<EntityReference, InstantiationManipulation> instantiationManipulations) {

			Set<Map.Entry<EntityReference, InstantiationManipulation>> entrySet = instantiationManipulations.entrySet();
			int size = entrySet.size();

			final Map<String, ChangeValueManipulation> additionalIdManipulations = new LinkedHashMap<>(size);

			// Iterate through all Instantiations and check whether an Id Property manipulations exists
			for (Map.Entry<EntityReference, InstantiationManipulation> instantationManipulation : entrySet) {
				EntityReference entityReference = instantationManipulation.getKey();
				if (!idManipulations.containsKey(entityReference)) {
					// No explicit id property assignment found for this entity reference.
					try {

						// Check whether IdGeneratorAssignment is configured.

						EntityType<GenericEntity> entityType = typeReflection.getEntityType(entityReference.getTypeSignature());

						GenerateIdWith generateWith = session.getModelAccessory().getMetaData() //
								.entityType(entityType) //
								.meta(GenerateIdWith.T) //
								.exclusive();

						if (generateWith == null)
							continue;

						IdGenerator<?> idGenerator = generateWith.getGenerator();
						BasicIdGeneratorContext ctx = new BasicIdGeneratorContext(session, context.getSystemSession(), entityType);
						Object idValue = idGenerator.generateId(ctx);

						if (idValue != null) {
							ChangeValueManipulation idManipulation = createIdManipulation(entityReference, idValue);

							EntityProperty ep = (EntityProperty) idManipulation.getOwner();
							String key = "".concat(ep.getReference().getTypeSignature()).concat("#").concat(ep.getReference().getId().toString());

							additionalIdManipulations.put(key, idManipulation);
						}

					} catch (Exception e) {
						throw new RuntimeException("Error while generating ids.", e);
					}
				}
			}

			return additionalIdManipulations;
		}
	}

	private CompoundManipulation combineManipulations(Manipulation manipulation, List<? extends Manipulation> manipulationsToAdd) {
		CompoundManipulation compoundManipulation = null;
		if (manipulation instanceof CompoundManipulation) {
			compoundManipulation = (CompoundManipulation) manipulation;
		} else {
			compoundManipulation = CompoundManipulation.T.create();
			compoundManipulation.setCompoundManipulationList(new ArrayList<Manipulation>());
			if (manipulation != null) {
				compoundManipulation.getCompoundManipulationList().add(manipulation);
			}
		}

		compoundManipulation.getCompoundManipulationList().addAll(manipulationsToAdd);
		return compoundManipulation;
	}

	private ChangeValueManipulation createIdManipulation(EntityReference entityReference, Object idValue) {
		ChangeValueManipulation idManipulation = ChangeValueManipulation.T.create();
		EntityProperty idProperty = EntityProperty.T.create();
		idProperty.setPropertyName(GenericEntity.id);
		idProperty.setReference(entityReference);
		idManipulation.setOwner(idProperty);
		idManipulation.setNewValue(idValue);
		return idManipulation;
	}

	private EntityProperty getEntityProperty(ChangeValueManipulation changeValueManipulation) {
		Owner owner = changeValueManipulation.getOwner();
		if (owner instanceof EntityProperty) {
			EntityProperty entityProperty = (EntityProperty) owner;
			return entityProperty;
		}
		return null;
	}

	private EntityReference getEntityReference(InstantiationManipulation instantiationManipulation) {
		GenericEntity entity = instantiationManipulation.getEntity();
		if (entity instanceof EntityReference) {
			return (EntityReference) instantiationManipulation.getEntity();
		} else {
			return entity.reference();
		}
	}

	private boolean isIdPropertyManipulation(EntityProperty entityProperty) {
		return GenericEntity.id.equals(entityProperty.getPropertyName());
	}

}
