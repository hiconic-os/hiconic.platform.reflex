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
package hiconic.rx.hibernate.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import javax.sql.DataSource;

import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.persistence.hibernate.dialects.HibernateDialectMappings;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.model.configuration.HibernatePersistenceConfiguration;
import hiconic.rx.hibernate.module.api.HibernateContract;
import hiconic.rx.hibernate.processing.DialectAutoSense;
import hiconic.rx.hibernate.processing.HibernatePersistences;
import hiconic.rx.hibernate.service.api.HibernatePersistence;
import hiconic.rx.hibernate.wire.contract.HibernatePropertiesContract;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class HibernateRxModuleSpace implements RxModuleContract, HibernateContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private HibernatePropertiesContract hibernateProperties;

	@Import
	private DatabaseContract database;

	@Override
	public HibernatePersistence persistence(HibernatePersistenceConfiguration configuration, ConfiguredModel configuredModel, DataSource dataSource) {
		return persistence(configuration, configuredModel.systemCmdResolver(), dataSource);
	}

	@Override
	public HibernatePersistence persistence(HibernatePersistenceConfiguration configuration, CmdResolver cmdResolver, DataSource dataSource) {
		return persistences().acquirePersistence(configuration, cmdResolver, dataSource);
	}

	@Override
	public HibernatePersistence mainPersistence() {
		return persistence(null, platform.configuredModels().mainPersistenceModel(), getOrTunnel(database.mainDataSource()));
	}

	@Managed
	private HibernatePersistences persistences() {
		HibernatePersistences bean = new HibernatePersistences();
		bean.setDebugOrmOutputFolder(hibernateProperties.ormDebugOutputFolder());
		bean.setDialectAutoSense(dialectAutoSense());
		return bean;
	}

	@Managed
	private DialectAutoSense dialectAutoSense() {
		DialectAutoSense bean = new DialectAutoSense();
		bean.setDialectMappings(HibernateDialectMappings.mapppings());
		return bean;
	}

}