package hiconic.rx.hibernate.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import javax.sql.DataSource;

import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.persistence.hibernate.dialects.HibernateDialectMappings;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.db.module.api.DatabaseContract;
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
	public HibernatePersistence persistence(CmdResolver cmdResolver, DataSource dataSource) {
		return persistences().acquirePersistence(cmdResolver, dataSource);
	}
	
	@Override
	public HibernatePersistence persistence(ConfiguredModel configuredModel, DataSource dataSource) {
		return persistences().acquirePersistence(configuredModel.cmdResolver(), dataSource);
	}
	
	@Override
	public HibernatePersistence mainPersistence() {
		return persistence(platform.configuredModels().mainPersistenceModel(), getOrTunnel(database.mainDataSource()));
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