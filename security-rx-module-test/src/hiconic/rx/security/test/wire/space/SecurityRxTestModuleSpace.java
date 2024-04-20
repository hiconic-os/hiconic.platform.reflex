package hiconic.rx.security.test.wire.space;

import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.security.model.test.SecurityTestRequest;
import hiconic.rx.security.test.processing.SecurityTestProcessor;

@Managed
public class SecurityRxTestModuleSpace implements RxModuleContract {

	@Override
	public void configureMainServiceDomain(ServiceDomainConfiguration configuration) {
		configuration.bindRequest(SecurityTestRequest.T, this::testProcessor);
	}
	
	@Managed
	private SecurityTestProcessor testProcessor() {
		SecurityTestProcessor bean = new SecurityTestProcessor();
		return bean;
	}
}
