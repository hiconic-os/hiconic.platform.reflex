package hiconic.rx.hibernate.test.wire.space;

import org.hibernate.SessionFactory;

import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._HibernateTestModel_;
import hiconic.rx.db.module.api.DatabaseContract;
import hiconic.rx.hibernate.module.api.HibernateContract;
import hiconic.rx.hibernate.service.api.HibernatePersistence;
import hiconic.rx.hibernate.test.model.api.GetPersons;
import hiconic.rx.hibernate.test.processing.PersonPersistenceProcessor;
import hiconic.rx.hibernate.test.wire.contract.HibernateTestContract;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
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
	public void configureMainPersistenceModel(ModelConfiguration configuration) {
		configuration.addModel(_HibernateTestModel_.reflection);
		configuration.configureModel(this::configureModel);
	}
	
	private void configureModel(ModelMetaDataEditor editor) {
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
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(GetPersons.T, () -> hibernate.mainPersistence().asServiceProcessor(processor()));
	}
}
