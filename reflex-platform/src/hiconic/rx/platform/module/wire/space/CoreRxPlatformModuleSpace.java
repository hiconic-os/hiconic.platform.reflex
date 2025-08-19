package hiconic.rx.platform.module.wire.space;

import com.braintribe.model.processing.service.common.CompositeServiceProcessor;
import com.braintribe.model.service.api.CompositeRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * Module that brings core ...
 */
@Managed
public class CoreRxPlatformModuleSpace implements RxModuleContract {

	// @formatter:off
	@Import private RxPlatformContract platform;
	// @formatter:on

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		configureSystemDomain(configurations);
		configureModelsDomain(configurations);
	}

	// ###############################################
	// ## . . . . . System Service Domain . . . . . ##
	// ###############################################

	private void configureSystemDomain(ServiceDomainConfigurations configurations) {
		configurations.system().bindRequest(CompositeRequest.T, this::compositeProcessor);
	}

	@Managed
	private CompositeServiceProcessor compositeProcessor() {
		CompositeServiceProcessor bean = new CompositeServiceProcessor();
		// TODO configure "swallowed" exceptions log level

		return bean;
	}

	// ###############################################
	// ## . . . . . Models Service Domain . . . . . ##
	// ###############################################

	// TODO not sure this will be needed, at least for now
	private void configureModelsDomain(ServiceDomainConfigurations configurations) {
	}

}
