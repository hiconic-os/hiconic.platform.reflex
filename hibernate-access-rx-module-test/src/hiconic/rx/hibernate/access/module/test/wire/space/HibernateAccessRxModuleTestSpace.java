package hiconic.rx.hibernate.access.module.test.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._HibernateTestModel_;
import hiconic.rx.module.api.service.ModelConfiguration;
import hiconic.rx.module.api.service.ModelConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class HibernateAccessRxModuleTestSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Override
	public void configureModels(ModelConfigurations configurations) {
		ModelConfiguration modelConfiguration = configurations.byName("rx-test:configured-main-access-model");
		modelConfiguration.addModel(_HibernateTestModel_.reflection);
	}
}