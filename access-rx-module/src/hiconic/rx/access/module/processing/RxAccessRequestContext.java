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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.common.attribute.TypeSafeAttribute;
import com.braintribe.common.attribute.TypeSafeAttributeEntry;
import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.manipulation.Manipulation;
import com.braintribe.model.generic.session.exception.GmSessionException;
import com.braintribe.model.generic.session.exception.GmSessionRuntimeException;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.service.api.ServiceRequestContextAspect;
import com.braintribe.model.processing.service.api.ServiceRequestContextBuilder;
import com.braintribe.model.processing.service.api.ServiceRequestSummaryLogger;
import com.braintribe.model.processing.session.api.persistence.CommitListener;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;
import com.braintribe.model.resource.CallStreamCapture;
import com.braintribe.model.resource.source.TransientSource;
import com.braintribe.model.service.api.ServiceRequest;

import hiconic.rx.access.api.AccessRequestContext;

public class RxAccessRequestContext<P extends AccessRequest> implements AccessRequestContext<P> {

	private final ServiceRequestContext delegate;
	private final PersistenceGmSessionFactory requestSessionFactory;
	private final PersistenceGmSessionFactory systemSesssionFactory;
	private final P originalRequest;
	private PersistenceGmSession systemSession;
	private PersistenceGmSession session;
	private P systemRequest;
	private P request;
	private final List<Manipulation> inducedManipulations = new ArrayList<>();
	private boolean autoInducing = true;

	public RxAccessRequestContext(ServiceRequestContext delegate, PersistenceGmSessionFactory requestSessionFactory,
			PersistenceGmSessionFactory systemSessionFactory, P originalRequest) {

		this.delegate = delegate;
		this.requestSessionFactory = requestSessionFactory;
		this.systemSesssionFactory = systemSessionFactory;
		this.originalRequest = originalRequest;
	}

	// @formatter:off
	@Override public void setAutoInducing(boolean autoInducing) { this.autoInducing = autoInducing; }
	@Override public <T> EvalContext<T> eval(ServiceRequest evaluable) { return delegate.eval(evaluable); }
	@Override public String getRequestorAddress() { return delegate.getRequestorAddress(); }
	@Override public String getRequestorId() { return delegate.getRequestorId(); }
	@Override public String getRequestorSessionId() { return delegate.getRequestorSessionId(); }
	@Override public String getRequestorUserName() { return delegate.getRequestorUserName(); }
	@Override public String getRequestedEndpoint() { return delegate.getRequestedEndpoint(); }
	@Override public boolean isAuthorized() { return delegate.isAuthorized(); }
	@Override public boolean isTrusted() { return delegate.isTrusted(); }
	@Override public String getDomainId() { return delegate.getDomainId(); }
	@Override public void notifyResponse(Object response) { delegate.notifyResponse(response); }
	@Override public ServiceRequestSummaryLogger summaryLogger() { return delegate.summaryLogger(); }
	// @formatter:on
	
	@Override
	public PersistenceGmSession getSystemSession() {
		if (systemSession == null) {
			try {
				systemSession = systemSesssionFactory.newSession(getAccessId());
			} catch (GmSessionException e) {
				throw new GmSessionRuntimeException("error while providing a system session for the request: " + originalRequest, e);
			}
		}
		return systemSession;
	}

	@Override
	public PersistenceGmSession getSession() {
		if (session == null) {
			try {
				session = requestSessionFactory.newSession(getAccessId());
				session.listeners().add(new ManipulationCollectingListener());
			} catch (GmSessionException e) {
				throw new GmSessionRuntimeException("error while providing a request session for the request: " + originalRequest, e);
			}
		}
		return session;
	}

	private String getAccessId() {
		return requireNonNull(originalRequest.getDomainId(),
				() -> "Cannot create session, " + originalRequest.entityType().getShortName() + ".domainId is null. Request: " + originalRequest);
	}

	@Override
	public P getOriginalRequest() {
		return originalRequest;
	}

	@Override
	public P getRequest() {
		if (request == null) {
			request = getSession().merge().adoptUnexposed(false).envelopeFactory(new TransientDataAwareEnvelopeFactory()).suspendHistory(true)
					.doFor(originalRequest);
		}
		return request;
	}

	@Override
	public P getSystemRequest() {
		if (systemRequest == null) {
			systemRequest = getSystemSession().merge().adoptUnexposed(false).envelopeFactory(new TransientDataAwareEnvelopeFactory())
					.suspendHistory(true).doFor(originalRequest);
		}
		return systemRequest;
	}

	private static class TransientDataAwareEnvelopeFactory implements Function<GenericEntity, GenericEntity> {

		@Override
		public GenericEntity apply(GenericEntity t) {
			if (t.hasTransientData()) {
				if (t instanceof TransientSource) {
					TransientSource transientSource = (TransientSource) t;
					TransientSource clonedTransientSource = (TransientSource) transientSource.entityType().create();
					clonedTransientSource.setInputStreamProvider(transientSource.getInputStreamProvider());
					return clonedTransientSource;
				} else if (t instanceof CallStreamCapture) {
					CallStreamCapture callStreamCapture = (CallStreamCapture) t;
					CallStreamCapture clonedCallStreamCapture = (CallStreamCapture) callStreamCapture.entityType().create();
					clonedCallStreamCapture.setOutputStreamProvider(callStreamCapture.getOutputStreamProvider());
					return clonedCallStreamCapture;
				}
			}

			return t.entityType().create();
		}

	}

	public List<PersistenceGmSession> getUsedSessions() {
		List<PersistenceGmSession> usedSessions = new ArrayList<>(2);
		if (systemSession != null)
			usedSessions.add(systemSession);

		if (session != null)
			usedSessions.add(session);

		return usedSessions;
	}

	protected void commitIfNecessary(PersistenceGmSession session) throws GmSessionException {
		if (session == null)
			return;

		if (session.getTransaction().hasManipulations()) {
			session.commit();
		}
	}

	public void commitIfNecessary() throws GmSessionException {
		commitIfNecessary(systemSession);
		commitIfNecessary(session);
	}

	public List<Manipulation> getInducedManipulations() {
		return inducedManipulations;
	}

	private class ManipulationCollectingListener implements CommitListener {

		@Override
		public void onBeforeCommit(PersistenceGmSession session, Manipulation manipulation) {
			// Nothing to do here
		}

		@Override
		public void onAfterCommit(PersistenceGmSession session, Manipulation manipulation, Manipulation inducedManipluation) {
			if (autoInducing) {
				if (manipulation != null) {
					inducedManipulations.add(manipulation);
				}
				if (inducedManipluation != null) {
					inducedManipulations.add(inducedManipluation);
				}
			}
		}

	}

	@Override
	public ServiceRequestContextBuilder derive() {
		return delegate.derive();
	}

	@Override
	public <T, A extends ServiceRequestContextAspect<? super T>> T findAspect(Class<A> aspect) {
		return delegate.findAspect(aspect);
	}

	@Override
	public void transferAttributes(Map<Class<? extends TypeSafeAttribute<?>>, Object> target) {
		delegate.transferAttributes(target);
	}

	@Override
	public AttributeContext parent() {
		return delegate.parent();
	}

	@Override
	public Stream<TypeSafeAttributeEntry> streamAttributes() {
		return delegate.streamAttributes();
	}

	@Override
	public <A extends TypeSafeAttribute<V>, V> Optional<V> findAttribute(Class<A> attribute) {
		return delegate.findAttribute(attribute);
	}

}
