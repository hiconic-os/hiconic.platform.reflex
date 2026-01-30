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

import com.braintribe.model.accessapi.AccessRequest;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;

public interface AccessRequestContext<P extends AccessRequest> extends ServiceRequestContext {

	PersistenceGmSession getSession();
	PersistenceGmSession getSystemSession();
	P getRequest();
	P getSystemRequest();
	P getOriginalRequest();
	
	/**
	 * enables/disables auto inducing on the request session ({@link #getSession()}. Default is true
	 */
	void setAutoInducing(boolean autoInducing);
}
