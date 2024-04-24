package hiconic.rx.access.hibernate.module.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.access.hibernate.model.configuration.HibernateAccess;
import hiconic.rx.access.hibernate.processing.HibernateAccessExpert;
import hiconic.rx.access.module.api.AccessExpertContract;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.module.api.HibernateContract;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class HibernateAccessRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;
	
	@Import
	private AccessExpertContract accessExpert;
	
	@Import
	private HibernateContract hibernate;
	
	@Import
	private DatabaseContract database;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		accessExpert.registerAccessExpert(HibernateAccess.T, hibernateAccessExpert());
	}
	
	@Managed
	private HibernateAccessExpert hibernateAccessExpert() {
		HibernateAccessExpert bean = new HibernateAccessExpert();
		bean.setDatabaseContract(database);
		bean.setHibernateContract(hibernate);
		return bean;
	}


}