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
package hiconic.rx.hibernate.processing;

import java.util.function.Supplier;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.mpc.ModelPathCondition;
import com.braintribe.model.processing.service.api.ReasonedServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.traversing.engine.GMT;
import com.braintribe.model.processing.traversing.engine.api.customize.ClonerCustomization;
import com.braintribe.model.processing.traversing.engine.api.usecase.AbsentifySkipUseCase;
import com.braintribe.model.processing.traversing.engine.impl.clone.BasicClonerCustomization;
import com.braintribe.model.processing.traversing.engine.impl.clone.Cloner;
import com.braintribe.model.processing.traversing.engine.impl.skip.conditional.MpcConfigurableSkipper;
import com.braintribe.model.processing.traversing.engine.impl.walk.BasicModelWalkerCustomization;
import com.braintribe.model.processing.traversing.engine.impl.walk.ModelWalker;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.service.api.PersistenceContext;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;

public class PersistenceAdapterServiceProcessor<I extends ServiceRequest, O> implements ReasonedServiceProcessor<I, O> {

	private final Supplier<SessionFactory> sessionFactorySupplier;
	private final PersistenceServiceProcessor<I, O> delegate;

	public PersistenceAdapterServiceProcessor(Supplier<SessionFactory> sessionFactorySupplier, PersistenceServiceProcessor<I, O> delegate) {
		this.sessionFactorySupplier = sessionFactorySupplier;
		this.delegate = delegate;
	}

	@Override
	public Maybe<? extends O> processReasoned(ServiceRequestContext context, I request) {
		SessionFactory sessionFactory = sessionFactorySupplier.get();
		Session session = sessionFactory.openSession();

		try {
			PersistenceContext persistenceContext = new PersistenceContextImpl(context, sessionFactory);
			return delegate.process(persistenceContext, session, request);

		} finally {
			session.close();
		}
	}

	private class PersistenceContextImpl implements PersistenceContext {
		private final ServiceRequestContext requestContext;
		private final SessionFactory sessionFactory;

		public PersistenceContextImpl(ServiceRequestContext requestContext, SessionFactory sessionFactory) {
			this.requestContext = requestContext;
			this.sessionFactory = sessionFactory;
		}

		@Override
		public ServiceRequestContext requestContext() {
			return requestContext;
		}

		@Override
		public SessionFactory sessionFactory() {
			return sessionFactory;
		}

		@Override
		public <T> T detach(T value, ModelPathCondition condition) {
			BasicModelWalkerCustomization basicCustomization = new BasicModelWalkerCustomization();
			basicCustomization.setAbsenceResolvable(false);
			basicCustomization.setAbsenceTraversable(true);

			ModelWalker modelWalker = new ModelWalker();
			modelWalker.setWalkerCustomization(basicCustomization);
			modelWalker.setBreadthFirst(true);

			MpcConfigurableSkipper skipper = new MpcConfigurableSkipper();
			skipper.setCondition(condition);
			skipper.setSkipUseCase(AbsentifySkipUseCase.INSTANCE);

			ClonerCustomization clonerCustomization = new BasicClonerCustomization();

			Cloner cloner = new Cloner();
			cloner.setCustomizer(clonerCustomization);

			GMT.traverse() //
					.customWalk(modelWalker) //
					.visitor(skipper) //
					.visitor(cloner) //
					.doFor(value);

			return cloner.getClonedValue();
		}
	}
}
