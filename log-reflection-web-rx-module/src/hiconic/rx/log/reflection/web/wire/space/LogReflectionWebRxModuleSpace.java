package hiconic.rx.log.reflection.web.wire.space;

import static com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling.getOrTunnel;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.braintribe.model.processing.service.api.ServiceAroundProcessor;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.log.reflection.model.api.LogRecordPage;
import hiconic.rx.log.reflection.model.api.QueryLogRecords;
import hiconic.rx.log.reflection.model.configuration.LogReflectionConfiguration;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebAppNavigationEntry;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.security.web.api.WebSecurityContract;
import hiconic.rx.web.server.api.attribute.RequestMetaLoggingAttribute;
import hiconic.rx.web.server.api.attribute.RequestMetaLoggingControl;

@Managed
public class LogReflectionWebRxModuleSpace implements RxModuleContract {
	public static final String WEBAPP_PATH = "log-reflection";

	@Import
	private WebServerContract webServer;

	@Import
	private RxPlatformContract platform;
	@Import
	private WebSecurityContract webSecurity;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		configurations.byId("logging").bindInterceptor("log-reflection-tail-request-meta-logging")
				.forType(QueryLogRecords.T).bind(this::tailRequestMetaLoggingInterceptor);
	}

	private ServiceAroundProcessor<QueryLogRecords, Object> tailRequestMetaLoggingInterceptor() {
		return (context, request, proceed) -> {
			Object response = proceed.proceed(request);
			if (response instanceof LogRecordPage && request.getCursor() != null && !request.getCursor().isBlank()) {
				RequestMetaLoggingControl control = context.findOrNull(RequestMetaLoggingAttribute.class);
				if (control != null)
					control.suppress();
			}
			return response;
		};
	}

	@Override
	public void onDeploy() {
		webServer.addWebAppRuntimeConfiguration(WEBAPP_PATH, this::runtimeConfiguration);
		Set<String> roles = readerRoles();
		webServer.addWebAppNavigation(new WebAppNavigationEntry(WEBAPP_PATH, "Log Reflection",
				"Inspect and filter the application log.", roles, 100));
	}

	private Map<String, Object> runtimeConfiguration() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("loginUrl", loginUrl());
		result.put("readerRoles", readerRoles());
		result.put("managerRoles", managerRoles());
		result.put("tailPollIntervalMillis", tailPollIntervalMillis());
		return result;
	}

	private int tailPollIntervalMillis() {
		return Math.max(100, configuration().getTailPollIntervalMillis());
	}

	private String loginUrl() {
		String path = webSecurity.defaultLoginPath();
		return webServer.defaultEndpointUrl() + (path.startsWith("/") ? path : "/" + path);
	}

	private Set<String> readerRoles() {
		Set<String> roles = new LinkedHashSet<>(configured(configuration().getReaderRoles()));
		roles.addAll(configured(configuration().getManagerRoles()));
		roles.addAll(platform.auth().roleAuthorization().adminRoles());
		return roles;
	}

	private Set<String> managerRoles() {
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
}
