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
package hiconic.rx.module.api.service;

import java.util.List;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.service.api.ServiceRequest;

public interface ServiceDomains {

	// @formatter:off
	/**Returns the main {@link ServiceDomain}*/
	default ServiceDomain main() { return byId("main"); }
	/**Returns the system {@link ServiceDomain}*/
	default ServiceDomain system() { return byId("system"); }
	// @formatter:on

	
	/** Returns the {@link ServiceDomain} for given domainId or <tt>null</tt> if no such domain exists. */
	ServiceDomain byId(String domainId);
	
	/**Lists all available {@link ServiceDomain}s */
	List<? extends ServiceDomain> list();

	List<? extends ServiceDomain> listDomains(EntityType<? extends ServiceRequest> requestType);

	List<? extends ServiceDomain> listDomains(GmMetaModel model);

}
