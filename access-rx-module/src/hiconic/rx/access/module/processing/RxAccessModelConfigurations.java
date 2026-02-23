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
package hiconic.rx.access.module.processing;

import com.braintribe.cfg.Required;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSessionFactory;

import hiconic.rx.access.module.api.AccessDataModelConfiguration;
import hiconic.rx.access.module.api.AccessDomains;
import hiconic.rx.access.module.api.AccessModelConfigurations;
import hiconic.rx.access.module.api.AccessServiceModelConfiguration;
import hiconic.rx.access.module.api.AccessSymbol;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomains;

public class RxAccessModelConfigurations implements AccessModelConfigurations {

	private ModelConfigurations modelConfigurations;
	private PersistenceGmSessionFactory contextSessionFactory;
	private PersistenceGmSessionFactory systemSessionFactory;

	public void initModelConfigurations(ModelConfigurations modelConfigurations) {
		this.modelConfigurations = modelConfigurations;
	}

	@Required
	public void setContextSessionFactory(PersistenceGmSessionFactory contextSessionFactory) {
		this.contextSessionFactory = contextSessionFactory;
	}

	@Required
	public void setSystemSessionFactory(PersistenceGmSessionFactory systemSessionFactory) {
		this.systemSessionFactory = systemSessionFactory;
	}

	@Override
	public AccessDataModelConfiguration dataModelConfiguration(AccessSymbol accessId) {
		return dataModelConfiguration(modelConfigurations.byName(AccessDomains.accessDataModelName(accessId)));
	}
	
	@Override
	public AccessDataModelConfiguration dataModelConfiguration(String accessId) {
		return dataModelConfiguration(modelConfigurations.byName(AccessDomains.accessDataModelName(accessId)));
	}

	@Override
	public AccessDataModelConfiguration dataModelConfiguration(ModelConfiguration modelConfiguration) {
		return new RxAccessDataModelConfiguration(modelConfiguration);
	}

	@Override
	public AccessServiceModelConfiguration serviceModelConfiguration(AccessSymbol accessId) {
		return serviceModelConfiguration(modelConfigurations.byName(ServiceDomains.serviceDomainModelName(accessId)));
	}

	@Override
	public AccessServiceModelConfiguration serviceModelConfiguration(String accessId) {
		return serviceModelConfiguration(modelConfigurations.byName(ServiceDomains.serviceDomainModelName(accessId)));
	}
	
	@Override
	public AccessServiceModelConfiguration serviceModelConfiguration(ModelConfiguration modelConfiguration) {
		return new RxAccessServiceModelConfiguration(modelConfiguration, contextSessionFactory, systemSessionFactory);
	}
}
