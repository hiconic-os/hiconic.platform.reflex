package hiconic.rx.log.reflection.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.LinkedHashSet;
import java.util.Set;

import com.braintribe.logging.level.LogLevelManager;
import com.braintribe.logging.level.LogLevelRuntimeUpdater;
import com.braintribe.logging.level.LogLevelSetup;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.log.reflection.model.api.InternalLogReflectionRequest;
import hiconic.rx.log.reflection.model.api.LogReflectionRequest;
import hiconic.rx.log.reflection.model.api.ManageLogReflectionRequest;
import hiconic.rx.log.reflection.model.api.ReadLogReflectionRequest;
import hiconic.rx.log.reflection.model.configuration.LogReflectionConfiguration;
import hiconic.rx.log.reflection.processing.LogReflectionAuthorizationInterceptor;
import hiconic.rx.log.reflection.processing.LogReflectionLogLevelUpdateDispatcher;
import hiconic.rx.log.reflection.processing.LogReflectionProcessor;
import hiconic.rx.log.reflection.processing.LogReflectionRequestLoggingContextualizer;
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
		logging.bindInterceptor("log-reflection-read-authorization").forType(ReadLogReflectionRequest.T)
				.bind(this::readAuthorizationInterceptor);
		logging.bindInterceptor("log-reflection-manage-authorization").forType(ManageLogReflectionRequest.T)
				.bind(this::manageAuthorizationInterceptor);
		logging.bindInterceptor("log-reflection-internal-authorization").forType(InternalLogReflectionRequest.T)
				.bind(this::internalAuthorizationInterceptor);
		logging.bindRequest(LogReflectionRequest.T, this::processor);
	}

	@Override
	public void onDeploy() {
		platform.serviceProcessing().requestContextualizers()
				.register(new LogReflectionRequestLoggingContextualizer(platform.application().logManager()));
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
		bean.setLogLevelManager(logLevelManager());
		bean.setLogLevelRuntimeUpdater(logLevelRuntimeUpdater());
		bean.setRoleAuthorization(platform.auth().roleAuthorization());
		bean.setReaderRoles(effectiveReaderRoles());
		bean.setManagerRoles(managerRoles());
		return bean;
	}

	public Set<String> readerRoles() {
		return configured(configuration().getReaderRoles());
	}

	public Set<String> managerRoles() {
		return configured(configuration().getManagerRoles());
	}

	public Set<String> effectiveReaderRoles() {
		Set<String> roles = new LinkedHashSet<>(readerRoles());
		roles.addAll(managerRoles());
		return roles;
	}

	private static Set<String> configured(Set<String> roles) {
		return roles == null ? Set.of() : Set.copyOf(roles);
	}

	@Managed
	private LogReflectionAuthorizationInterceptor readAuthorizationInterceptor() {
		return authorizationInterceptor(effectiveReaderRoles(), "A log reflection reader role is required");
	}

	@Managed
	private LogReflectionAuthorizationInterceptor manageAuthorizationInterceptor() {
		return authorizationInterceptor(managerRoles(), "A log reflection manager role is required");
	}

	@Managed
	private LogReflectionAuthorizationInterceptor internalAuthorizationInterceptor() {
		return authorizationInterceptor(Set.of(security.internalRole()), "An internal RX identity is required");
	}

	private LogReflectionAuthorizationInterceptor authorizationInterceptor(Set<String> roles, String message) {
		LogReflectionAuthorizationInterceptor bean = new LogReflectionAuthorizationInterceptor();
		bean.setRoleAuthorization(platform.auth().roleAuthorization());
		bean.setRoles(roles);
		bean.setMessage(message);
		return bean;
	}

	@Managed
	private LogLevelManager logLevelManager() {
		return LogLevelSetup.instance().logLevelManager();
	}

	@Managed
	private LogLevelRuntimeUpdater logLevelRuntimeUpdater() {
		LogLevelRuntimeUpdater bean = new LogLevelRuntimeUpdater();
		bean.setLogLevelManager(logLevelManager());
		bean.setUpdateDispatcher(logLevelUpdateDispatcher());
		return bean;
	}

	@Managed
	private LogReflectionLogLevelUpdateDispatcher logLevelUpdateDispatcher() {
		LogReflectionLogLevelUpdateDispatcher bean = new LogReflectionLogLevelUpdateDispatcher();
		bean.setEvaluator(platform.serviceProcessing().systemEvaluator());
		bean.setLocalInstanceId(platform.application().instanceId());
		bean.setLogLevelManager(logLevelManager());
		return bean;
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
		bean.setCapacity(configuration().getLiveLogCapacity());
		bean.setLeaseMillis(configuration().getLiveLogLeaseMillis());
		return bean;
	}

	@Managed
	private LogbackLogStreamInventory inventory() {
		LogbackLogStreamInventory bean = new LogbackLogStreamInventory();
		bean.setInstanceId(platform.application().instanceId());
		return bean;
	}
}
