package hiconic.rx.log.reflection.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.LinkedHashSet;
import java.util.Set;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.log.reflection.model.api.LogReflectionRequest;
import hiconic.rx.log.reflection.model.configuration.LogReflectionConfiguration;
import hiconic.rx.log.reflection.processing.LogReflectionProcessor;
import hiconic.rx.log.reflection.processing.LogbackLogStreamInventory;
import hiconic.rx.log.reflection.processing.LogFileReader;
import hiconic.rx.log.reflection.processing.LogBundleWriter;
import hiconic.rx.log.reflection.processing.StructuredLiveLogCollector;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.security.api.SecurityContract;

@Managed
public class LogReflectionRxModuleSpace implements RxModuleContract {
	public static final String LOGGING_DOMAIN = "logging";

	@Import
	private RxPlatformContract platform;
	@Import
	private SecurityContract security;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		var logging = configurations.byId(LOGGING_DOMAIN);
		logging.setDisplayName("Logging");
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
		bean.setReaderRoles(readerRoles());
		bean.setManagerRoles(managerRoles());
		bean.setInternalRole(security.internalRole());
		return bean;
	}

	public Set<String> readerRoles() {
		Set<String> roles = new LinkedHashSet<>(configured(configuration().getReaderRoles()));
		roles.addAll(configured(configuration().getManagerRoles()));
		roles.addAll(platform.auth().roleAuthorization().adminRoles());
		return roles;
	}

	public Set<String> managerRoles() {
		Set<String> roles = new LinkedHashSet<>(configured(configuration().getManagerRoles()));
		roles.addAll(platform.auth().roleAuthorization().adminRoles());
		return roles;
	}

	private static Set<String> configured(Set<String> roles) {
		return roles == null ? Set.of() : roles;
	}

	@Managed
	private LogReflectionConfiguration configuration() {
		return getOrTunnel(platform.configuration().readConfig(LogReflectionConfiguration.T));
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
		return bean;
	}
}
