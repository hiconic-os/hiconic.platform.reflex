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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;

import org.hibernate.Session;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.hibernate.annotations.PersistenceService;
import hiconic.rx.hibernate.annotations.TransactionMode;
import hiconic.rx.hibernate.service.api.PersistenceContext;
import hiconic.rx.hibernate.service.api.PersistenceDispatchConfiguration;
import hiconic.rx.hibernate.service.api.PersistenceDispatching;
import hiconic.rx.hibernate.service.api.PersistenceProcessor;

public class PersistenceProcessorDispatching<P extends ServiceRequest, R> implements PersistenceDispatching<P, R> {

	private final PersistenceProcessor<P, R> persistenceProcessor;

	static <P extends ServiceRequest, R> PersistenceProcessorDispatching<P, R> create(PersistenceProcessor<P, R> persistenceProcessor) {
		return new PersistenceProcessorDispatching<>(persistenceProcessor);
	}

	public PersistenceProcessorDispatching(PersistenceProcessor<P, R> persistenceProcessor) {
		this.persistenceProcessor = persistenceProcessor;
	}

	@Override
	public void bind(PersistenceDispatchConfiguration<P, R> dispatching) {
		Lookup lookup = MethodHandles.lookup();
		
		for (Method method : persistenceProcessor.getClass().getMethods()) {
			PersistenceService transaction = method.getAnnotation(PersistenceService.class);
			if (transaction == null)
				continue;

			TransactionMode transactionMode = transaction.value();

			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes.length != 3)
				throw new UnsupportedOperationException("method " + method
						+ " is annotated with Transaction but does not comply to required signature (PersistenceContext, Session, ServiceRequest");

			Class<?> persistenceContextParameterType = parameterTypes[0];
			Class<?> sessionParameterType = parameterTypes[1];
			Class<?> requestParameterType = parameterTypes[2];

			if (persistenceContextParameterType != PersistenceContext.class)
				throw new UnsupportedOperationException("method " + method
						+ " is annotated with Transaction but does not comply to required signature (PersistenceContext, Session, ServiceRequest");

			if (sessionParameterType != Session.class)
				throw new UnsupportedOperationException("method " + method
						+ " is annotated with Transaction but does not comply to required signature (PersistenceContext, Session, ServiceRequest");

			if (!ServiceRequest.class.isAssignableFrom(requestParameterType))
				throw new UnsupportedOperationException("method " + method
						+ " is annotated with Transaction but does not comply to required signature (PersistenceContext, Session, ServiceRequest");

			try {
				MethodHandle methodHandle = lookup.unreflect(method);
				MethodHandle boundHandle = methodHandle.bindTo(persistenceProcessor);

				EntityType<P> requestType = EntityTypes.T((Class<P>) requestParameterType);

				dispatching.register(requestType, transactionMode, new MethodHandlePersistenceServiceProcessor<P, R>(boundHandle));

			} catch (Error e) {
				throw e;

			} catch (Throwable e) {
				throw new IllegalStateException( //
						"Unexpected Exception when converting Transaction annotated method to PersistenceServiceProcessor", e);
			}

		}
	}
}
