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
package hiconic.rx.access.api;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

public abstract class AbstractStatefulAccessRequestProcessor<P extends AccessRequest, R> {
	
	protected AccessRequestContext<P> context;

	public abstract Maybe<R> process();

	public void initContext(AccessRequestContext<P> context) {
		this.context = context;
		configure();
	}
	
	protected void configure() {
		// noop;
	}

	public AccessRequestContext<P> context() {
		return context;
	}
	
	public PersistenceGmSession session() {
		return context.getSession();
	}
	
	public PersistenceGmSession systemSession() {
		return context.getSystemSession();
	}
	
	public P request() {
		return context.getRequest();
	}
	
	public P systemRequest() {
		return context.getSystemRequest();
	}
	
}
