// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.cli.processing;

import java.util.Map;
import java.util.stream.Collectors;

import com.braintribe.cfg.Required;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.service.api.ServiceProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.module.api.service.ServiceDomains;
import hiconic.rx.platform.cli.model.api.ReflectServiceDomains;
import hiconic.rx.platform.cli.model.api.ServiceDomainsDescription;

public class ReflectServiceDomainsProcessor implements ServiceProcessor<ReflectServiceDomains, ServiceDomainsDescription> {

	private ServiceDomains serviceDomains;

	@Required
	public void setServiceDomains(ServiceDomains serviceDomains) {
		this.serviceDomains = serviceDomains;
	}

	@Override
	public ServiceDomainsDescription process(ServiceRequestContext context, ReflectServiceDomains request) {
		ServiceDomainsDescription result = ServiceDomainsDescription.T.create();
		result.setDomainNameToModel(indexDomains());

		return result;
	}

	private Map<String, GmMetaModel> indexDomains() {
		return serviceDomains.list().stream() //
				.collect(Collectors.toMap( //
						sd -> sd.domainId(), sd -> sd.modelOracle().getGmMetaModel() //
				));
	}

}
