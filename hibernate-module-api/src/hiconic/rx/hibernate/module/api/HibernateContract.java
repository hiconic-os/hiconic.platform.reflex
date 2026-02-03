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
package hiconic.rx.hibernate.module.api;

import javax.sql.DataSource;

import com.braintribe.model.processing.meta.cmd.CmdResolver;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.model.configuration.HibernatePersistenceConfiguration;
import hiconic.rx.hibernate.service.api.HibernatePersistence;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxExportContract;

public interface HibernateContract extends RxExportContract {

	HibernatePersistence persistence(HibernatePersistenceConfiguration configuration, ConfiguredModel configuredModel, DataSource dataSource);

	HibernatePersistence persistence(HibernatePersistenceConfiguration configuration, CmdResolver cmdResolver, DataSource dataSource);

	/**
	 * Returns a {@link HibernatePersistence} using {@link DatabaseContract#mainDataSource()} Database configuration and the Model acquired by
	 * {@link ModelConfigurations#mainPersistenceModel()}
	 */
	HibernatePersistence mainPersistence(); //
}
