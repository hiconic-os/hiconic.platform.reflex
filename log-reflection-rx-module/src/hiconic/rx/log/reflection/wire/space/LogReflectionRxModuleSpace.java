package hiconic.rx.log.reflection.wire.space;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.log.reflection.model.api.LogReflectionRequest;
import hiconic.rx.log.reflection.processing.LogReflectionProcessor;
import hiconic.rx.log.reflection.processing.LogbackLogStreamInventory;
import hiconic.rx.log.reflection.processing.LogFileReader;
import hiconic.rx.log.reflection.processing.LogBundleWriter;
import hiconic.rx.log.reflection.processing.StructuredLiveLogCollector;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;

@Managed
public class LogReflectionRxModuleSpace implements RxModuleContract {
	public static final String LOGGING_DOMAIN = "logging";

	@Import
	private RxPlatformContract platform;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		var logging = configurations.byId(LOGGING_DOMAIN);
		logging.setDisplayName("Logging");
		logging.allowRoles(platform.auth().roleAuthorization().adminRoles());
		logging.bindRequest(LogReflectionRequest.T, this::processor);
	}

	@Managed
	private LogReflectionProcessor processor() {
		LogReflectionProcessor bean = new LogReflectionProcessor();
		bean.setInstanceId(platform.application().instanceId());
		bean.setCollector(collector());
		bean.setInventory(inventory());
		bean.setFileReader(fileReader());
		bean.setBundleWriter(new LogBundleWriter());
		bean.setSystemEvaluator(platform.serviceProcessing().systemEvaluator());
		bean.setLiveInstances(platform.application().liveInstances());
		return bean;
	}

	@Managed
	private LogFileReader fileReader() {
		return new LogFileReader();
	}

	@Managed
	private StructuredLiveLogCollector collector() {
		StructuredLiveLogCollector bean = new StructuredLiveLogCollector();
		bean.setInstanceId(platform.application().instanceId());
		return bean;
	}

	@Managed
	private LogbackLogStreamInventory inventory() {
		LogbackLogStreamInventory bean = new LogbackLogStreamInventory();
		bean.setInstanceId(platform.application().instanceId());
		bean.setProcessProtocolPath(platform.processLaunch().protocolOutputPath());
		return bean;
	}
}
