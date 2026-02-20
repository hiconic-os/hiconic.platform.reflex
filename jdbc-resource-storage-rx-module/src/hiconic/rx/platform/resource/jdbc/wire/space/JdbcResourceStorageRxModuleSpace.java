package hiconic.rx.platform.resource.jdbc.wire.space;

import javax.sql.DataSource;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.resource.source.SqlSource;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.module.api.resource.ResourceStorage;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformConfigurator;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.resource.jdbc.processing.JdbcResourceStorage;

/**
 * This modules registers a deployment expert for JdbcResourceStorage.
 * 
 * @see JdbcResourceStorage
 * @see hiconic.rx.resource.jdbc.model.configuration.JdbcResourceStorage JdbcResourceStorage
 * @see ResourceStorage
 */
@Managed
public class JdbcResourceStorageRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private DatabaseContract database;

	@Override
	public void configurePlatform(RxPlatformConfigurator configurator) {
		configurator.registerResourceStorageDeploymentExpert( //
				hiconic.rx.resource.jdbc.model.configuration.JdbcResourceStorage.T, SqlSource.T, this::deployJdbcResourceStorage);
	}

	@Managed
	private Maybe<ResourceStorage> deployJdbcResourceStorage(hiconic.rx.resource.jdbc.model.configuration.JdbcResourceStorage storageDenotation) {
		DataSource dataSource = database.dataSource(storageDenotation.getDataSourceId()).get();

		JdbcResourceStorage result = new JdbcResourceStorage();
		result.setStorageId(storageDenotation.getStorageId());
		result.setDataSource(dataSource);
		result.setTableName(storageDenotation.getTableName());
		result.setIdColumnName(storageDenotation.getIdColumnName());
		result.setBlobColumnName(storageDenotation.getBlobColumnName());

		result.postConstruct();

		return Maybe.complete(result);
	}

}