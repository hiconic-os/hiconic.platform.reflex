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
package hiconic.rx.security.test.wire.space;

import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.security.model.test.SecurityTestRequest;
import hiconic.rx.security.test.processing.SecurityTestProcessor;

@Managed
public class SecurityRxTestModuleSpace implements RxModuleContract {

	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(SecurityTestRequest.T, this::testProcessor);
	}
	
	@Managed
	private SecurityTestProcessor testProcessor() {
		SecurityTestProcessor bean = new SecurityTestProcessor();
		return bean;
	}
}
