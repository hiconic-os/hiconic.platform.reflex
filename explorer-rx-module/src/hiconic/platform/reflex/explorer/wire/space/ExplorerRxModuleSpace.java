package hiconic.platform.reflex.explorer.wire.space;

import com.braintribe.model.bapi.AvailableAccessesRequest;
import com.braintribe.model.bapi.CurrentUserInformationRequest;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import hiconic.platform.reflex.explorer.processing.bapi.AvailableAccessesProcessor;
import hiconic.platform.reflex.explorer.processing.bapi.CurrentUserInformationProcessor;
import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.module.api.service.ServiceDomainConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * Module that brings support for tribefire-explorer
 */
@Managed
public class ExplorerRxModuleSpace implements RxModuleContract {

	// @formatter:off
	@Import private RxPlatformContract platform;

	@Import private AccessContract access;

	@Import private WebappsSpace webapps;
	@Import private CortexSpace cortex;
	// @formatter:on

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		ServiceDomainConfiguration explorerSd = configurations.byId("explorer");

		explorerSd.bindRequest(AvailableAccessesRequest.T, this::availableAccessesProcessor);
		explorerSd.bindRequest(CurrentUserInformationRequest.T, this::currentUserInformationProcessor);
	}

	@Managed
	private AvailableAccessesProcessor availableAccessesProcessor() {
		AvailableAccessesProcessor bean = new AvailableAccessesProcessor();
		bean.setAccessDomains(access.accessDomains());
		return bean;
	}

	@Managed
	private CurrentUserInformationProcessor currentUserInformationProcessor() {
		CurrentUserInformationProcessor bean = new CurrentUserInformationProcessor();
		return bean;
	}
	
	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		webapps.registerWebapps();
	}

	@Override
	public void onDeploy() {
		cortex.registerCortexAccess();
	}
}
