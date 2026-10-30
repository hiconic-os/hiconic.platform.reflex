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
package hiconic.rx.access.smood.module.test.wire.space;

import java.util.List;

import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.util.meta.NewMetaModelGeneration;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.smood.module.test.context.AttributeContextMarker;
import hiconic.rx.access.smood.module.test.context.AttributeContextProbeAspect;
import hiconic.rx.access.smood.module.test.context.ReadAttributeContextMarker;
import hiconic.rx.access.smood.module.test.wire.contract.SmoodAccessContextTestContract;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;

@Managed
public class SmoodAccessContextTestModuleSpace implements RxModuleContract, SmoodAccessContextTestContract {
	@Import
	private AccessContract access;

	@Override
	public void configureModels(ModelConfigurations configurations) {
		access.accessModelConfigurations().dataModelConfiguration("main-access")
				.bindAspect("attribute-context-probe").bind(this::attributeContextProbeAspect);
	}

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		var accessDomain = configurations.byId("main-access");
		accessDomain.addModel(new NewMetaModelGeneration().buildMetaModel(
				"hiconic.platform.reflex:attribute-context-test-model", List.of(ReadAttributeContextMarker.T),
				List.of(ServiceRequest.T.getModel().getMetaModel())));
		accessDomain.bindRequest(ReadAttributeContextMarker.T,
				() -> (context, request) -> context.findOrNull(AttributeContextMarker.class));
	}

	@Managed
	public AttributeContextProbeAspect attributeContextProbeAspect() {
		return new AttributeContextProbeAspect();
	}
}
