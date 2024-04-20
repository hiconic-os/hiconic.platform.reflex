package hiconic.rx.hibernate.module.api;

import javax.sql.DataSource;

import com.braintribe.model.processing.meta.cmd.CmdResolver;

import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.service.api.HibernatePersistence;
import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxExportContract;

public interface HibernateContract extends RxExportContract {

	HibernatePersistence persistence(ConfiguredModel configuredModel, DataSource dataSource);
	
	HibernatePersistence persistence(CmdResolver cmdResolver, DataSource dataSource);
	
	/**
	 * Returns a {@link HibernatePersistence} using {@link DatabaseContract#mainDataSource()} Database configuration and the Model acquired by {@link ModelConfigurations#mainPersistenceModel()
	 */
	HibernatePersistence mainPersistence(); //   
}
