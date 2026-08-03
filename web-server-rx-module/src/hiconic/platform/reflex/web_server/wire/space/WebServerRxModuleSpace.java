// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.platform.reflex.web_server.wire.space;

import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.utils.lcd.CollectionTools2.newConcurrentSet;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.xnio.Options;

import com.braintribe.gm.logging.level.LogLevelApplicationResolver;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.logging.level.servlet.LogLevelServlet;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.provider.Box;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.context.WireContextConfiguration;

import dev.hiconic.servlet.api.remote.RemoteClientAddressResolver;
import dev.hiconic.servlet.impl.remote.StandardRemoteClientAddressResolver;
import hiconic.platform.reflex.web_server.processing.ApplicationStateGateHandler;
import hiconic.platform.reflex.web_server.processing.DelegatingAuthenticationContextFilter;
import hiconic.platform.reflex.web_server.processing.DefaultRxServlet;
import hiconic.platform.reflex.web_server.processing.InstanceEndpointConfigurator;
import hiconic.platform.reflex.web_server.processing.PackagedPublicResourceServlet;
import hiconic.platform.reflex.web_server.processing.ReflexAccessLogReceiver;
import hiconic.platform.reflex.web_server.processing.RoleAuthorizationFilter;
import hiconic.platform.reflex.web_server.processing.RuntimeConfigurationHandler;
import hiconic.platform.reflex.web_server.processing.SsePushServlet;
import hiconic.platform.reflex.web_server.processing.SslConfig;
import hiconic.platform.reflex.web_server.processing.WebAppRegistry;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.push.api.PushContract;
import hiconic.rx.web.server.api.FilterSymbol;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.web.server.api.WebServerFilters;
import hiconic.rx.web.server.model.config.StaticFilesystemResourceMapping;
import hiconic.rx.web.server.model.config.StaticWebServerConfiguration;
import hiconic.rx.web.server.model.config.WebServerConfiguration;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.Undertow.ListenerInfo;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import io.undertow.util.URLUtils;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * This module's javadoc is yet to be written.
 */
@Managed
public class WebServerRxModuleSpace implements RxModuleContract, WebServerContract {

	@Import
	private RxPlatformContract platform;

	@Import
	private FiltersSpace filters;

	@Import
	private PushContract push;

	@Override
	@Managed
	public RemoteClientAddressResolver remoteAddressResolver() {
		StandardRemoteClientAddressResolver resolver = new StandardRemoteClientAddressResolver();
		resolver.setIncludeForwarded(true);
		resolver.setIncludeXForwardedFor(true);
		resolver.setIncludeXRealIp(true);
		resolver.setLenientParsing(true);
		return resolver;
	}

	@Override
	public void onLoaded(WireContextConfiguration configuration) {
		platform.application().logManager().setLogLevel("io.undertow.request.error-response", System.Logger.Level.INFO);
		registerPushTransports();
		registerPackagedPublicResources();
		registerLogLevelServlet();
		undertowServer().start();

		WebServerConfiguration config = configuration();
		println( //
				sequence( //
						text("Web Server running. "), //
						cyan("URL:"), //
						text(" http://" + config.getHostName() + ":" + config.getPort()) //
				) //
		);

		SslConfig sslConfig = sslConfigBox().value;
		if (sslConfig != null) {
			println( //
					sequence( //
							text("HTTPS enabled. "), //
							cyan("URL:"), //
							text(" https://" + config.getHostName() + ":" + sslConfig.port()) //
					) //
			);
		}
	}

	private void registerPushTransports() {
		SsePushServlet sse = ssePushServlet();
		push.addHandler(sse);
		addServlet("sse-push", pushSseEndpointPath(), sse);
	}

	private String pushSseEndpointPath() {
		return URLUtils.normalizeSlashes(configuration().getPushSseEndpointPath());
	}

	@Managed
	private SsePushServlet ssePushServlet() {
		SsePushServlet bean = new SsePushServlet();
		bean.setMarshallerRegistry(platform.marshalling().marshallers());
		bean.setEvaluator(platform.serviceProcessing().systemEvaluator());
		bean.setProcessingInstanceId(platform.application().instanceId());
		bean.setPushContract(push);
		return bean;
	}

	private void registerPackagedPublicResources() {
		addServlet("/", "packaged-public-resources", "/res/*", packagedPublicResourceServlet());
	}

	@Managed
	private PackagedPublicResourceServlet packagedPublicResourceServlet() {
		PackagedPublicResourceServlet bean = new PackagedPublicResourceServlet();
		bean.setResources(platform.packagedPublicResources());
		return bean;
	}

	private void registerLogLevelServlet() {
		addServlet("log-levels", "/log-levels", logLevelServlet());
		addFilter(WebServerFilters.authenticationContext, authenticationContextFilter());
		addFilterMapping(WebServerFilters.authenticationContext, "/log-levels", DispatcherType.REQUEST);
		addFilterMapping(WebServerFilters.authenticationContext, "/log-levels/*", DispatcherType.REQUEST);
		addFilter(LogLevelFilters.logLevelAdmin, logLevelAdminFilter());
		addFilterMapping(LogLevelFilters.logLevelAdmin, "/log-levels", DispatcherType.REQUEST);
		addFilterMapping(LogLevelFilters.logLevelAdmin, "/log-levels/*", DispatcherType.REQUEST);
	}

	private enum LogLevelFilters implements FilterSymbol {
		logLevelAdmin
	}

	@Managed
	private LogLevelServlet logLevelServlet() {
		LogLevelServlet bean = new LogLevelServlet();
		bean.setEvaluator(platform.serviceProcessing().systemEvaluator());
		bean.setLocalInstanceId(platform.application().instanceId());
		bean.setApplicationResolver(logLevelApplicationResolver());
		return bean;
	}

	@Managed
	private RoleAuthorizationFilter logLevelAdminFilter() {
		return new RoleAuthorizationFilter(platform.auth()::roleAuthorization);
	}

	@Override
	public void bindAuthenticationContextDelegate(Filter delegate) {
		authenticationContextFilter().bindDelegate(delegate);
	}

	@Managed
	private DelegatingAuthenticationContextFilter authenticationContextFilter() {
		return new DelegatingAuthenticationContextFilter(() -> platform.auth().roleAuthorization().securityActive());
	}

	@Managed
	private LogLevelApplicationResolver logLevelApplicationResolver() {
		return new LogLevelApplicationResolver() {
			@Override
			public Set<String> liveApplications() {
				return platform.application().liveInstances().liveApplications();
			}

			@Override
			public Maybe<InstanceId> resolveApplication(String applicationId) {
				InstanceId localInstanceId = platform.application().instanceId();
				if (applicationId == null || applicationId.equals(localInstanceId.getApplicationId())) {
					return Maybe.complete(localInstanceId);
				}

				Set<String> instances = platform.application().liveInstances().liveInstances(InstanceId.of(null, applicationId));
				if (instances == null || instances.isEmpty()) {
					return Maybe.empty(InvalidArgument.create("No live instance found for application: " + applicationId));
				}

				return Maybe.complete(InstanceId.parse(instances.iterator().next()));
			}
		};
	}

	@Override
	public void addServlet(String name, String path, HttpServlet servlet) {
		addServlet(defaultEndpointsBasePath(), name, path, servlet);
	}

	@Override
	public void addServlet(String basePath, String name, String path, HttpServlet servlet) {
		ServletInfo servletInfo = Servlets.servlet(name, servlet.getClass(), new ImmediateInstanceFactory<>(servlet));
		servletInfo.setAsyncSupported(true);
		servletInfo.addMapping(path);
		deploymentInfo(normalizeBasePath(basePath)).addServlet(servletInfo);
	}

	@Override
	public Supplier<ServletContext> servletContextSupplier() {
		return servletContextSupplier(defaultEndpointsBasePath());
	}

	@Override
	public Supplier<ServletContext> servletContextSupplier(String basePath) {
		return () -> {
			// TODO check if we are already in onApplicationReady, otherwise throw an exception because servlets are still being registered
			return servletDeploymentManager(normalizeBasePath(basePath)).getDeployment().getServletContext();
		};
	}

	@Override
	public void addStaticFileResource(String path, String rootDir, String... welcomeFiles) {
		addStaticFileResource(applicationHandler(), path, rootDir, welcomeFiles);
	}

	@Override
	public void addWebAppRuntimeConfiguration(String webAppPath, Supplier<? extends Map<String, ?>> properties) {
		String normalizedWebAppPath = normalizeWebAppPath(webAppPath);
		webAppRegistry().register(normalizedWebAppPath);
		String normalizedPath = URLUtils.normalizeSlashes("/" + normalizedWebAppPath + "/runtime-config.json");
		applicationHandler().addExactPath(normalizedPath,
				new RuntimeConfigurationHandler(() -> {
					Map<String, Object> result = new java.util.LinkedHashMap<>();
					result.put("servicesUrl", defaultEndpointUrl());
					result.putAll(properties.get());
					return result;
				}));
	}

	@Override
	public boolean isWebAppRegistered(String webAppPath) {
		return webAppRegistry().isRegistered(normalizeWebAppPath(webAppPath));
	}

	@Managed
	private WebAppRegistry webAppRegistry() {
		return new WebAppRegistry();
	}

	private static String normalizeWebAppPath(String webAppPath) {
		String normalized = URLUtils.normalizeSlashes("/" + webAppPath);
		return normalized.substring(1).replaceFirst("/$", "");
	}

	@Override
	public void addPackagedPublicResources(String name, String path, String resourcePathPrefix) {
		PackagedPublicResourceServlet servlet = new PackagedPublicResourceServlet();
		servlet.setResources(platform.packagedPublicResources());
		servlet.setResourcePathPrefix(resourcePathPrefix);
		String mapping = URLUtils.normalizeSlashes("/" + path + "/*");
		addServlet(name, mapping, servlet);
	}

	private void addStaticFileResource(PathHandler pathHandler, String path, String rootDir, String... welcomeFiles) {
		ResourceHandler resourceHandler = new ResourceHandler(new FileResourceManager(new File(rootDir), 100)) //
				.setWelcomeFiles(welcomeFiles) //
				.setDirectoryListingEnabled(false);

		pathHandler.addPrefixPath(path, resourceHandler);
	}

	@Override
	public void addFilter(FilterSymbol name, Filter filter) {
		addFilter(defaultEndpointsBasePath(), name, filter);
	}

	@Override
	public void addFilter(String basePath, FilterSymbol name, Filter filter) {
		FilterInfo filterInfo = Servlets.filter(name.name(), filter.getClass(), new ImmediateInstanceFactory<>(filter));
		filterInfo.setAsyncSupported(true);
		deploymentInfo(normalizeBasePath(basePath)).addFilter(filterInfo);
	}

	@Override
	public void addFilterMapping(String filterName, String mapping, DispatcherType dispatcherType) {
		addFilterMapping(defaultEndpointsBasePath(), filterName, mapping, dispatcherType);
	}

	@Override
	public void addFilterMapping(String basePath, String filterName, String mapping, DispatcherType dispatcherType) {
		deploymentInfo(normalizeBasePath(basePath)).addFilterUrlMapping(filterName, mapping, dispatcherType);
	}

	@Override
	public void addFilterServletNameMapping(String filterName, String mapping, DispatcherType dispatcherType) {
		addFilterServletNameMapping(defaultEndpointsBasePath(), filterName, mapping, dispatcherType);
	}

	@Override
	public void addFilterServletNameMapping(String basePath, String filterName, String mapping, DispatcherType dispatcherType) {
		deploymentInfo(normalizeBasePath(basePath)).addFilterServletNameMapping(filterName, mapping, dispatcherType);
	}

	@Override
	public void addEndpoint(String path, Endpoint endpoint) {
		addEndpoint(defaultEndpointsBasePath(), path, endpoint);
	}

	@Override
	public String pushWebSocketEndpointPath() {
		return URLUtils.normalizeSlashes(configuration().getPushWebSocketEndpointPath());
	}

	public void addEndpoint(String basePath, String path, Endpoint endpoint) {
		registerWsEndpoint(wsDeploymentInfo(normalizeBasePath(basePath)), path, endpoint);
	}

	private void registerWsEndpoint(WebSocketDeploymentInfo deploymentInfo, String path, Endpoint endpoint) {
		ServerEndpointConfig serverEndpointConfig = ServerEndpointConfig.Builder.create(endpoint.getClass(), path)
				.configurator(new InstanceEndpointConfigurator(endpoint)).build();

		deploymentInfo.addEndpoint(serverEndpointConfig);
	}

	@Override
	public String callerInfoFilterName() {
		return "caller-info";
	}

	@Override
	public int getEffectiveServerPort() {
		Undertow server = undertowServer();
		List<ListenerInfo> listenerInfo = server.getListenerInfo();

		if (listenerInfo.isEmpty())
			return -1;

		InetSocketAddress address = (InetSocketAddress) listenerInfo.get(0).getAddress();
		return address.getPort();
	}

	@Override
	@Managed
	public String publicUrl() {
		// DO NOT INLINE !!!
		return resolvePublicUrl();
	}

	private String resolvePublicUrl() {
		WebServerConfiguration config = configuration();

		String result = config.getPublicUrl();
		if (result == null) {
			SslConfig sslConfig = sslConfigBox().value;
			if (sslConfig != null)
				result = "https://" + config.getHostName() + ":" + sslConfig.port();
			 else
				result = "http://" + config.getHostName() + ":" + config.getPort();
		}

		return result;
	}

	@Override
	public boolean isSslEnabled() {
		return sslConfigBox().value != null;
	}

	@Override
	@Managed
	public String defaultEndpointUrl() {
		return resolvedDfaultEndpointUrl();
	}

	private String resolvedDfaultEndpointUrl() {
		String result = publicUrl() + "/" + resolveDefaultEndpointPath("");
		while (result.endsWith("/"))
			result = result.substring(0, result.length() - 1);

		return result;
	}

	@Override
	public String resolveDefaultEndpointPath(String path) {
		String defaultBasePath = defaultEndpointsBasePath();
		if (defaultBasePath == null)
			return path;
		else
			return defaultEndpointsBasePath() + URLUtils.normalizeSlashes(path);
	}

	@Override
	public void onApplicationReady() {
		PathHandler applicationHandler = applicationHandler();
		configureApplicationHandler(applicationHandler);
		applicationStateGateHandler().setStandardHandler(applicationHandler);
	}

	private void configureApplicationHandler(PathHandler applicationHandler) {
		addResourceHandlers(applicationHandler);
		addEndpointsHandlers(applicationHandler);
	}

	@Managed
	private PathHandler applicationHandler() {
		return Handlers.path();
	}

	private void addEndpointsHandlers(PathHandler pathHandler) {
		for (String basePath : basePaths())
			try {
				pathHandler.addPrefixPath(basePath, servletDeploymentManager(basePath).start());
			} catch (ServletException e) {
				throw new RuntimeException(e);
			}
	}

	private void addResourceHandlers(PathHandler pathHandler) {
		for (StaticFilesystemResourceMapping mapping : webServerConfiguration().getResourceMappings()) {
			addStaticFileResource( //
					pathHandler, //
					mapping.getPath(), //
					mapping.getRootDir(), //
					// TODO: ask Peter about "index.html" in old code
					mapping.getWelcomeFiles().toArray(new String[0]) //
			);

			// old code
			// ResourceHandler resourceHandler = new ResourceHandler(new FileResourceManager(new File(mapping.getRootDir()), 100)) //
			// .setWelcomeFiles("index.html") //
			// .setDirectoryListingEnabled(false);
			//
			// pathHandler.addPrefixPath(mapping.getPath(), resourceHandler);
		}
	}

	@Managed
	private ApplicationStateGateHandler applicationStateGateHandler() {
		WebServerConfiguration configuration = configuration();
		String healthEndpointAliasBasePath = configuration.getExposeHealthEndpointsAtDefaultEndpointsBasePath()
				? defaultEndpointsBasePath()
				: null;
		ApplicationStateGateHandler bean = new ApplicationStateGateHandler(platform.application().stateManager(), healthEndpointAliasBasePath);
		return bean;
	}

	@Managed
	private Undertow undertowServer() {
		WebServerConfiguration configuration = configuration();

		Builder builder = Undertow.builder() //
				.addHttpListener(configuration.getPort(), "0.0.0.0") //
				.setHandler(rootHandler());

		if (configuration.getAccessLogEnabled())
			builder.setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true);

		Integer ioThreads = configuration.getIoThreads();
		if (ioThreads != null)
			builder.setIoThreads(ioThreads);

		Integer coreThreads = configuration.getCoreThreads();
		if (coreThreads != null)
			builder.setWorkerOption(Options.WORKER_TASK_CORE_THREADS, coreThreads);

		Integer maxThreads = configuration.getMaxThreads();
		if (maxThreads != null)
			builder.setWorkerOption(Options.WORKER_TASK_MAX_THREADS, maxThreads);

		Integer maxConnections = configuration.getMaxConnections();
		if (maxConnections != null) {
			// XNIO uses a high/low watermark pair to suspend and resume accepts.
			// Equal values model a strict maximum without an arbitrary hysteresis band.
			builder.setWorkerOption(Options.CONNECTION_HIGH_WATER, maxConnections);
			builder.setWorkerOption(Options.CONNECTION_LOW_WATER, maxConnections);
		}

		SslConfig sslConfig = sslConfigBox().value;
		if (sslConfig != null)
			builder.addHttpsListener(sslConfig.port(), "0.0.0.0", sslConfig.sslContext());

		Undertow bean = builder.build();
		return bean;
	}

	private HttpHandler rootHandler() {
		return configuration().getAccessLogEnabled() ? accessLogHandler() : applicationStateGateHandler();
	}

	@Managed
	private Box<SslConfig> sslConfigBox() {
		return Box.of(SslConfig.buildFromConfig(configuration(), platform.configuration()));
	}

	@Managed
	private AccessLogHandler accessLogHandler() {
		AccessLogHandler bean = new AccessLogHandler(applicationStateGateHandler(), logReceiver(), configuration().getAccessLogFormat(),
				Undertow.class.getClassLoader());
		return bean;
	}

	@Managed
	private ReflexAccessLogReceiver logReceiver() {
		ReflexAccessLogReceiver bean = new ReflexAccessLogReceiver();
		return bean;
	}

	private StaticWebServerConfiguration webServerConfiguration() {
		return platform.configuration().readConfig(StaticWebServerConfiguration.T).get();
	}

	@Managed
	private DeploymentManager servletDeploymentManager(String basePath) {
		DeploymentManager manager = Servlets.defaultContainer() //
				.addDeployment(deploymentInfo(basePath));
		manager.deploy();

		return manager;
	}

	@Managed
	private DeploymentInfo deploymentInfo(String basePath) {
		WebServerConfiguration configuration = configuration();

		DeploymentInfo bean = Servlets.deployment() //
				.setClassLoader(Undertow.class.getClassLoader()) //
				.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsDeploymentInfo(basePath)) //
				.setContextPath(basePath) //
				.setDeploymentName("servlet-deployment-" + basePath.replace("/", "-"));

		registerFilter(bean, "servlet-response-capture-filters", filters.captureFilter(), "/*", DispatcherType.REQUEST);
		registerFilter(bean, callerInfoFilterName(), filters.callerInfoFilter(), "/*", DispatcherType.REQUEST);
		registerFilter(bean, "exception-filters", filters.exceptionFilter(), "/*", DispatcherType.REQUEST);
		registerFilter(bean, "thread-renamer-filters", filters.threadRenamerFilter(), "/*", DispatcherType.REQUEST);

		if (configuration.getCorsConfiguration() != null)
			registerFilter(bean, "cors", filters.corsFilter(), "/*", DispatcherType.REQUEST);

		return bean;
	}

	@Managed
	private String defaultEndpointsBasePath() {
		WebServerConfiguration configuration = configuration();
		String value = configuration.getDefaultEndpointsBasePath();

		validateDefaultEndpointsBasePath(value);

		return value;
	}

	private void validateDefaultEndpointsBasePath(String path) {
		if (path == null)
			return;

		if (path.endsWith("/"))
			UnsatisfiedMaybeTunneling.tunnel(InvalidArgument.create(WebServerConfiguration.T.getShortName() + "."
					+ WebServerConfiguration.defaultEndpointsBasePath + " cannot end with '/'. Value: " + path));
	}

	private String normalizeBasePath(String path) {
		if (path == null)
			path = "/";
		else
			// ensure starts with a '/' and doesn't end with one
			path = URLUtils.normalizeSlashes(path);

		basePaths().add(path);
		return path;
	}

	private WebServerConfiguration configuration() {
		return platform.configuration().readConfig(WebServerConfiguration.T).get();
	}

	private void registerFilter(DeploymentInfo deploymentInfo, String name, Filter filter, String pathMapping, DispatcherType dispatcherType) {
		FilterInfo filterInfo = Servlets.filter(name, filter.getClass(), new ImmediateInstanceFactory<>(filter));
		filterInfo.setAsyncSupported(true);
		deploymentInfo.addFilter(filterInfo);
		deploymentInfo.addFilterUrlMapping(name, pathMapping, dispatcherType);
	}

	@Managed
	// basePath works as a cache key here, as the method is @Managed
	private WebSocketDeploymentInfo wsDeploymentInfo(@SuppressWarnings("unused") /* DO NOT DELETE!!! */ String basePath) {
		WebSocketDeploymentInfo bean = new WebSocketDeploymentInfo();
		return bean;
	}

	// UNUSED
	@Managed
	private DefaultRxServlet defaultServlet() {
		DefaultRxServlet bean = new DefaultRxServlet();
		bean.setApplicationName(platform.application().applicationName());

		return bean;
	}

	@Managed
	private Set<String> basePaths() {
		return newConcurrentSet();
	}

}
