package hiconic.rx.access.module.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.function.Predicate;
import java.util.function.Supplier;

import com.braintribe.common.attribute.AttributeContext;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.processing.service.api.ServiceInterceptorProcessor;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.utils.collection.impl.AttributeContexts;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.rx.access.model.configuration.Access;
import hiconic.rx.access.model.configuration.AccessConfiguration;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.access.module.api.AccessExpert;
import hiconic.rx.access.module.api.AccessExpertContract;
import hiconic.rx.access.module.processing.RxAccessModelConfigurations;
import hiconic.rx.access.module.processing.RxAccesses;
import hiconic.rx.access.module.processing.RxPersistenceGmSessionFactory;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class AccessRxModuleSpace implements RxModuleContract, AccessContract, AccessExpertContract {

	@Import
	private RxPlatformContract platform;
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		AccessConfiguration accessConfiguration = getOrTunnel(platform.readConfig(AccessConfiguration.T));
		
		for (Access access: accessConfiguration.getAccesses()) {
			deploy(access);
		}
	}
	
	@Override
	public void configureModels(ModelConfigurations configurations) {
		accesses().initModelConfigurations(configurations);
		accessModelConfigurations().initModelConfigurations(configurations);
	}
	
	@Override
	@Managed
	public RxAccessModelConfigurations accessModelConfigurations() {
		return new RxAccessModelConfigurations();
	}
	
	@Override
	public <A extends Access> void registerAccessExpert(EntityType<A> accessType, AccessExpert<A> expert) {
		accesses().registerExpert(accessType, expert);
	}
	
	@Override
	public void deploy(Access access) {
		accesses().deploy(access);
	}

	@Override
	@Managed
	public RxPersistenceGmSessionFactory contextSessionFactory() {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(AttributeContexts::peek);
		configure(bean);
		return bean;
	}
	
	@Override
	public RxPersistenceGmSessionFactory sessionFactory(AttributeContext attributeContext) {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(() -> attributeContext);
		configure(bean);
		return bean;
	}
	
	@Override
	@Managed
	public RxPersistenceGmSessionFactory systemSessionFactory() {
		RxPersistenceGmSessionFactory bean = new RxPersistenceGmSessionFactory();
		bean.setAttributeContextSupplier(platform.systemAttributeContextSupplier());
		configure(bean);
		return bean;
	}
	
	private void configure(RxPersistenceGmSessionFactory bean) {
		bean.setAccesses(accesses());
		bean.setEvaluatorSupplier(platform::evaluator);
	}
	
	@Managed
	private RxAccesses accesses() {
		RxAccesses bean = new RxAccesses();
		bean.setConfiguredModels(platform.configuredModels());
		bean.setContextSessionFactory(contextSessionFactory());
		bean.setSystemSessionFactory(systemSessionFactory());
		return bean;
	}
}