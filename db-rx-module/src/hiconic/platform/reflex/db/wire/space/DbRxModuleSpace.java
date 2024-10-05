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
package hiconic.platform.reflex.db.wire.space;

import javax.sql.DataSource;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.zaxxer.hikari.HikariDataSource;

import hiconic.platform.reflex.db.impl.HikariDataSources;
import hiconic.rx.db.model.configuration.Database;
import hiconic.rx.db.model.configuration.DatabaseConfiguration;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * Implements the {@link DatabaseContract} using {@link HikariDataSource} implementation.
 * <p>
 * {@link DataSource}s can be resolved by name, based on the {@link DatabaseConfiguration} entity.
 * <p>
 * (Note that this is a standard configuration entity, resolved via {@link RxPlatformContract#readConfig(EntityType)}.
 */
@Managed
public class DbRxModuleSpace implements RxModuleContract, DatabaseContract {

	private static final String DATABASE_NAME_MAIN = "main-db";

	@Import
	private RxPlatformContract platform;

	@Override
	public DataSource findDataSource(String name) {
		return dataSources().findDataSource(name);
	}

	@Override
	public Maybe<DataSource> dataSource(String name) {
		return dataSources().dataSource(name).cast();
	}

	@Override
	public DataSource deploy(Database database) {
		return dataSources().dataSource(database);
	}

	@Managed
	private HikariDataSources dataSources() {
		HikariDataSources bean = new HikariDataSources();
		bean.setDatabaseConfiguration(platform.readConfig(DatabaseConfiguration.T).get());
		return bean;
	}

	@Override
	public Maybe<DataSource> mainDataSource() {
		return dataSource(DATABASE_NAME_MAIN);
	}

}