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

import java.util.Set;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.access.AbstractDelegatingAccess;
import com.braintribe.model.access.ModelAccessException;
import com.braintribe.model.accessapi.CustomPersistenceRequest;
import com.braintribe.model.accessapi.ManipulationRequest;
import com.braintribe.model.accessapi.ManipulationResponse;
import com.braintribe.model.accessapi.ReferencesRequest;
import com.braintribe.model.accessapi.ReferencesResponse;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.query.EntityQueryResult;
import com.braintribe.model.query.PropertyQuery;
import com.braintribe.model.query.PropertyQueryResult;
import com.braintribe.model.query.SelectQuery;
import com.braintribe.model.query.SelectQueryResult;
import com.braintribe.utils.collection.impl.AttributeContexts;

/**
 * Keeps the attribute context of a persistence session active while the underlying access is
 * called. This is essential for equivalent sessions used on worker threads: AOP accesses create
 * their interceptor sessions from the current attribute context.
 */
public class ContextualizedIncrementalAccess extends AbstractDelegatingAccess {

	private final AttributeContext attributeContext;

	public ContextualizedIncrementalAccess(com.braintribe.model.access.IncrementalAccess delegate, AttributeContext attributeContext) {
		setDelegate(delegate);
		this.attributeContext = attributeContext;
	}

	@Override
	public GmMetaModel getMetaModel() {
		return scopedUnchecked(super::getMetaModel);
	}

	@Override
	public ManipulationResponse applyManipulation(ManipulationRequest request) throws ModelAccessException {
		return scoped(() -> super.applyManipulation(request));
	}

	@Override
	public EntityQueryResult queryEntities(EntityQuery request) throws ModelAccessException {
		return scoped(() -> super.queryEntities(request));
	}

	@Override
	public PropertyQueryResult queryProperty(PropertyQuery request) throws ModelAccessException {
		return scoped(() -> super.queryProperty(request));
	}

	@Override
	public SelectQueryResult query(SelectQuery request) throws ModelAccessException {
		return scoped(() -> super.query(request));
	}

	@Override
	public ReferencesResponse getReferences(ReferencesRequest request) throws ModelAccessException {
		return scoped(() -> super.getReferences(request));
	}

	@Override
	public Set<String> getPartitions() throws ModelAccessException {
		return scoped(super::getPartitions);
	}

	@Override
	public Object processCustomRequest(ServiceRequestContext context, CustomPersistenceRequest request) {
		return scopedUnchecked(() -> super.processCustomRequest(context, request));
	}

	private <T> T scoped(AccessOperation<T> operation) throws ModelAccessException {
		AttributeContexts.push(attributeContext);
		try {
			return operation.get();
		} finally {
			AttributeContexts.pop();
		}
	}

	private <T> T scopedUnchecked(UncheckedAccessOperation<T> operation) {
		AttributeContexts.push(attributeContext);
		try {
			return operation.get();
		} finally {
			AttributeContexts.pop();
		}
	}

	@FunctionalInterface
	private interface AccessOperation<T> {
		T get() throws ModelAccessException;
	}

	@FunctionalInterface
	private interface UncheckedAccessOperation<T> {
		T get();
	}
}
