package hiconic.rx.log.reflection.web.wire.space;

import com.braintribe.model.processing.service.api.ServiceAroundProcessor;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.log.reflection.model.api.LogRecordPage;
import hiconic.rx.log.reflection.model.api.QueryLogRecords;
import hiconic.rx.module.api.service.ServiceDomainConfigurations;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebAppNavigationEntry;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.web.server.api.attribute.RequestMetaLoggingAttribute;
import hiconic.rx.web.server.api.attribute.RequestMetaLoggingControl;

@Managed
public class LogReflectionWebRxModuleSpace implements RxModuleContract {
	public static final String WEBAPP_PATH = "log-reflection";

	@Import
	private WebServerContract webServer;

	@Import
	private RxPlatformContract platform;

	@Override
	public void configureServiceDomains(ServiceDomainConfigurations configurations) {
		configurations.byId("logging").bindInterceptor("log-reflection-tail-request-meta-logging")
				.forType(QueryLogRecords.T).bind(this::tailRequestMetaLoggingInterceptor);
	}

	private ServiceAroundProcessor<QueryLogRecords, Object> tailRequestMetaLoggingInterceptor() {
		return (context, request, proceed) -> {
			Object response = proceed.proceed(request);
			if (response instanceof LogRecordPage && request.getWaitMillis() > 0
					&& request.getCursor() != null && !request.getCursor().isBlank()) {
				RequestMetaLoggingControl control = context.findOrNull(RequestMetaLoggingAttribute.class);
				if (control != null)
					control.suppress();
			}
			return response;
		};
	}

	@Override
	public void onDeploy() {
		webServer.addWebAppRuntimeConfiguration(WEBAPP_PATH,
				() -> java.util.Map.of("adminRoles", platform.auth().roleAuthorization().adminRoles()));
		webServer.addWebAppNavigation(new WebAppNavigationEntry(WEBAPP_PATH, "Log Reflection",
				"Inspect and filter the application log.", platform.auth().roleAuthorization().adminRoles(), 100));
	}
}
