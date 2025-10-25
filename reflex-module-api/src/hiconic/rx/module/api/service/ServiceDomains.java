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

/**
 * Registry of all {@link ServiceDomain}s.
 */
public interface ServiceDomains {

	// @formatter:off
	/** @see PlatformServiceDomains#main */
	default ServiceDomain main() { return byId(PlatformServiceDomains.main); }
	/** @see PlatformServiceDomains#system */
	default ServiceDomain system() { return byId(PlatformServiceDomains.system); }
	// @formatter:on

	/** Returns the {@link ServiceDomain} for given domainId or <tt>null</tt> if no such domain exists. */
	ServiceDomain byId(String domainId);

	/** Returns the {@link ServiceDomain} for given {@link ServiceDomainSymbol} or <tt>null</tt> if no such domain exists. */
	ServiceDomain byId(ServiceDomainSymbol domainId);

	/** Lists all available {@link ServiceDomain}s */
	List<ServiceDomain> list();

	/** Lists all {@link ServiceDomain}s which contain given request type. */
	List<ServiceDomain> listDomains(EntityType<? extends ServiceRequest> requestType);

	/** Lists all {@link ServiceDomain}s which contain given model. */
	List<ServiceDomain> listDomains(GmMetaModel model);

}
