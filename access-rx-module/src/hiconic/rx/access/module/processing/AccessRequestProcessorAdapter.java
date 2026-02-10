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
package hiconic.rx.access.module.processing;

import static com.braintribe.model.generic.typecondition.TypeConditions.isKind;
import static com.braintribe.model.generic.typecondition.TypeConditions.or;

import java.util.List;
import java.util.function.Predicate;

import com.braintribe.model.accessapi.AccessManipulationRequest;
import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.accessapi.HasInducedManipulation;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.manipulation.util.ManipulationBuilder;
import com.braintribe.model.generic.pr.AbsenceInformation;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.pr.criteria.matching.StandardMatcher;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.generic.reflection.BaseType;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.StandardCloningContext;
import com.braintribe.model.generic.reflection.StrategyOnCriterionMatch;
import com.braintribe.model.generic.typecondition.basic.TypeKind;
import com.braintribe.model.processing.accessrequest.api.AccessRequestProcessor;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;

public class AccessRequestProcessorAdapter<P extends AccessRequest, R> implements ServiceProcessor<P, R> {

	private static TraversingCriterion sessionDataDefaultTc = null;
	private final AccessRequestProcessor<P, R> delegate;
	private final PersistenceGmSessionFactory contextSessionFactory;
	private final PersistenceGmSessionFactory systemSessionFactory;

	public AccessRequestProcessorAdapter(AccessRequestProcessor<P, R> delegate, PersistenceGmSessionFactory contextSessionFactory,
			PersistenceGmSessionFactory systemSessionFactory) {
		this.delegate =  delegate;
		this.contextSessionFactory = contextSessionFactory;
		this.systemSessionFactory = systemSessionFactory;
	}

	@Override
	public R process(ServiceRequestContext context, P request) {
		var accessContext = new RxAccessRequestContext<>(context, contextSessionFactory, systemSessionFactory, request);

		R response = delegate.process(accessContext);

		accessContext.commitIfNecessary();

		response = postProcessAndDetach(response, accessContext);

		return response;
	}

	private TraversingCriterion getSessionDataDefaultTc() {
		if (sessionDataDefaultTc == null) {
			sessionDataDefaultTc = TC.create().conjunction().property()
					.typeCondition(or(isKind(TypeKind.collectionType), isKind(TypeKind.entityType))).close().done();

		}
		return sessionDataDefaultTc;
	}

	private R postProcessAndDetach(R response, RxAccessRequestContext<P> context) {
		List<PersistenceGmSession> usedSessions = context.getUsedSessions();

		if (usedSessions.isEmpty())
			return response;

		TraversingCriterion sessionDataTc = getSessionDataDefaultTc();

		EnvelopeCloningContext cloningContext = new EnvelopeCloningContext(sessionDataTc, e -> usedSessions.contains(e.session()));

		if (context.getOriginalRequest() instanceof AccessManipulationRequest && response instanceof HasInducedManipulation) {
			HasInducedManipulation hasInducedManipulation = (HasInducedManipulation) response;
			cloningContext.setHasInducedManipulation(hasInducedManipulation);

			if (hasInducedManipulation.getInducedManipulation() == null) {
				List<Manipulation> inducedManipulations = context.getInducedManipulations();

				switch (inducedManipulations.size()) {
					case 0:
						break;
					case 1:
						hasInducedManipulation.setInducedManipulation(inducedManipulations.get(0));
						break;
					default:
						hasInducedManipulation.setInducedManipulation(ManipulationBuilder.compound(inducedManipulations));
				}
			}
		}

		response = BaseType.INSTANCE.clone(cloningContext, response, StrategyOnCriterionMatch.partialize);

		return response;
	}

	private static class EnvelopeCloningContext extends StandardCloningContext {
		private final SessionDataCloningContext sessionDataCloningContext;
		private final Predicate<GenericEntity> isSessionEntity;
		private HasInducedManipulation hasInducedManipulation;

		public EnvelopeCloningContext(TraversingCriterion sessionDataTc, Predicate<GenericEntity> isSessionEntity) {
			this.isSessionEntity = isSessionEntity;
			sessionDataCloningContext = new SessionDataCloningContext(sessionDataTc);
		}

		public void setHasInducedManipulation(HasInducedManipulation hasInducedManipulation) {
			this.hasInducedManipulation = hasInducedManipulation;
		}

		@Override
		public <T> T getAssociated(GenericEntity entity) {
			T associated = super.getAssociated(entity);

			if (associated != null)
				return associated;

			if (isSessionEntity.test(entity)) {
				associated = (T) entity.clone(sessionDataCloningContext);
				registerAsVisited(entity, associated);
			}

			return associated;
		}

		@Override
		public GenericEntity supplyRawClone(EntityType<? extends GenericEntity> entityType, GenericEntity instanceToBeCloned) {
			return instanceToBeCloned;
		}

		@Override
		public boolean canTransferPropertyValue(EntityType<? extends GenericEntity> entityType, Property property, GenericEntity instanceToBeCloned,
				GenericEntity clonedInstance, AbsenceInformation sourceAbsenceInformation) {

			return !(instanceToBeCloned == hasInducedManipulation && property.getName().equals("inducedManipulation"));
		}
	}

	private static class SessionDataCloningContext extends StandardCloningContext {

		public SessionDataCloningContext(TraversingCriterion sessionDataTc) {
			StandardMatcher matcher = new StandardMatcher();
			matcher.setCriterion(sessionDataTc);
			setMatcher(matcher);
			setAbsenceResolvable(true);
		}

	}

}
