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
package hiconic.rx.demo.wire.space;

import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.demo.model.api.PersonRequest;
import hiconic.rx.demo.model.api.ReverseText;
import hiconic.rx.demo.processing.DataGenerationSource;
import hiconic.rx.demo.processing.PersonRequestProcessor;
import hiconic.rx.demo.processing.ReverseTextProcessor;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class DemoRxModuleSpace implements RxModuleContract {
	
	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(ReverseText.T, this::reverseTextProcessor);
		configuration.bindRequest(PersonRequest.T, this::personRequestProcessor);
	}
	
	@Managed
	private ReverseTextProcessor reverseTextProcessor() {
		return new ReverseTextProcessor();
	}
	
	@Managed
	private PersonRequestProcessor personRequestProcessor() {
		PersonRequestProcessor bean = new PersonRequestProcessor();
		bean.setDataGenerationSourceSupplier(this::dataGenerationSource);
		return bean;
	}
	
	@Managed
	private DataGenerationSource dataGenerationSource() {
		return new DataGenerationSource();
	}
}
