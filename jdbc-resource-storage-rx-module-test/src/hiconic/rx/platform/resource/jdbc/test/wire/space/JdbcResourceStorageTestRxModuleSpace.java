// ============================================================================
package hiconic.rx.platform.resource.jdbc.test.wire.space;

import com.braintribe.gm._BasicResourceModel_;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex._ResourceStorageApiModel_;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.platform.resource.jdbc.test.JdbcResourceStorageTestDomains;
import hiconic.rx.platform.resource.jdbc.test.wire.contract.JdbcResourceStorageTestContract;

@Managed
public class JdbcResourceStorageTestRxModuleSpace implements RxModuleContract, JdbcResourceStorageTestContract {

	@Import
	private RxPlatformContract platform;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration sd = configurations.byId(JdbcResourceStorageTestDomains.jdbcResources);
		sd.addModel(_ResourceStorageApiModel_.reflection);
		sd.addModel(_BasicResourceModel_.reflection);
	}

}
