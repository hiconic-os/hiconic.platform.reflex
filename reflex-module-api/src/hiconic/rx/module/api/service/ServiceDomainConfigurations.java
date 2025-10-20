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

import hiconic.rx.module.api.wire.RxModuleContract;

/**
 * Allows configuring new and existing {@link ServiceDomain}s.
 * <p>
 * Passed to the module as an argument during initialization, via the method:
 * {@link RxModuleContract#configureServiceDomains(ServiceDomainConfigurations)}.
 */
public interface ServiceDomainConfigurations {

	/**
	 * Acquires a {@link ServiceDomainConfiguration} for given domainId. This means this method could potentially register a new {@link ServiceDomain}
	 * in the platform.
	 */
	// TODO apply some constraints for domainId, e.g. no special characters
	ServiceDomainConfiguration byId(String domainId);

	/** {@link ServiceDomainSymbol} equivalent of {@link #byId(String)} */
	ServiceDomainConfiguration byId(ServiceDomainSymbol domainId);

	/** @see PlatformServiceDomains#main */
	ServiceDomainConfiguration main();
	/** @see PlatformServiceDomains#system */
	ServiceDomainConfiguration system();

}
