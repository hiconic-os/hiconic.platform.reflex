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
package hiconic.rx.hibernate.test.wire.space;

import org.hibernate.SessionFactory;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._HibernateTestModel_;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.module.api.HibernateContract;
import hiconic.rx.hibernate.service.api.HibernatePersistence;
import hiconic.rx.hibernate.test.model.api.GetPersonByName;
import hiconic.rx.hibernate.test.model.api.GetPersons;
import hiconic.rx.hibernate.test.processing.PersonPersistenceProcessor;
import hiconic.rx.hibernate.test.wire.contract.HibernateTestContract;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class HibernateRxTestModuleSpace implements RxModuleContract, HibernateTestContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private HibernateContract hibernate;

	@Import
	private DatabaseContract database;

	@Override
	public void configureModels(ModelConfigurations configurations) {
		ModelConfiguration mainPersistenceModelConfig = configurations.mainPersistenceModel();

		mainPersistenceModelConfig.addModel(_HibernateTestModel_.reflection);
	}

	@Override
	public SessionFactory mainSessionFactory() {
		return hibernate.mainPersistence().sessionFactory();
	}

	@Managed
	public PersonPersistenceProcessor processor() {
		return new PersonPersistenceProcessor();
	}

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration mainDomainConfig = configurations.main();

		HibernatePersistence mainPersistence = hibernate.mainPersistence();

		mainDomainConfig.bindRequest(GetPersons.T, //
				() -> mainPersistence.asServiceProcessor(processor()));

		mainDomainConfig.bindRequest(GetPersonByName.T, //
				() -> mainPersistence.queryProcessor(GetPersonByName.T, "from Person where name = :name"));
	}
}
