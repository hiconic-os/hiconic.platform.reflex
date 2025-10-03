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
package hiconic.rx.platform.service;

import java.util.List;

import com.braintribe.cfg.Required;

import hiconic.rx.module.api.service.ServiceDomainConfigurations;

public class RxServiceDomainConfigurations implements ServiceDomainConfigurations {
	private RxServiceDomains serviceDomains;

	@Required
	public void setServiceDomains(RxServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}
	
	@Override
	public RxServiceDomain byId(String domainId) {
		return serviceDomains.acquire(domainId);
	}

	// @formatter:off
	@Override public RxServiceDomain main() { return byId("main"); }
	@Override public RxServiceDomain system() { return byId("system"); }
	@Override public RxServiceDomain models() { return byId("models"); }
	// @formatter:on
	
	public List<RxServiceDomain> list() {
		return serviceDomains.list();
	}
}
