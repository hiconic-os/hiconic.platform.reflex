package hiconic.platform.reflex.db.wire.space;

import javax.sql.DataSource;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.db.impl.HikariDataSources;
import hiconic.rx.db.model.configuration.DatabaseConfiguration;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class DbRxModuleSpace implements RxModuleContract, DatabaseContract {

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
	
	@Managed
	private HikariDataSources dataSources() {
		HikariDataSources bean = new HikariDataSources();
		bean.setDatabaseConfiguration(platform.readConfig(DatabaseConfiguration.T).get());
		return bean;
	}
}