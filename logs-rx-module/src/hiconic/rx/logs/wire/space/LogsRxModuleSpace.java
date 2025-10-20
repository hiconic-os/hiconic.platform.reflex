package hiconic.rx.logs.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.logs.model.api.LogsRequest;
import hiconic.rx.logs.processing.LogsRxProcessor;
import hiconic.rx.logs.processing.LogsServiceDomain;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

/**
 * Binds service processor for {@link LogsRequest}, to the service domain named {@link LogsServiceDomain#logs}.
 */
@Managed
public class LogsRxModuleSpace implements RxModuleContract {

	@Import
	private RxPlatformContract platform;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		configurations.byId(LogsServiceDomain.logs).bindRequest(LogsRequest.T, this::logsProcessor);
	}

	@Managed
	private LogsRxProcessor logsProcessor() {
		LogsRxProcessor bean = new LogsRxProcessor();
		return bean;
	}
}