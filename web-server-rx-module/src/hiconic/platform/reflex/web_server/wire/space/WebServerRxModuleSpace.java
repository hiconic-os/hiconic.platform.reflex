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
import java.util.Set;

import com.braintribe.gm.model.reason.UnsatisfiedMaybeTunneling;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import dev.hiconic.servlet.api.remote.RemoteClientAddressResolver;
import dev.hiconic.servlet.impl.remote.StandardRemoteClientAddressResolver;
import hiconic.platform.reflex.web_server.processing.DefaultRxServlet;
import hiconic.platform.reflex.web_server.processing.InstanceEndpointConfigurator;
import hiconic.platform.reflex.web_server.processing.ReflexAccessLogReceiver;
import hiconic.platform.reflex.web_server.processing.SslConfig;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.FilterSymbol;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.web.server.model.config.StaticFilesystemResourceMapping;
import hiconic.rx.web.server.model.config.StaticWebServerConfiguration;
import hiconic.rx.web.server.model.config.WebServerConfiguration;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.Undertow.ListenerInfo;
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
	public void addServlet(String name, String path, HttpServlet servlet) {
		addServlet(defaultEndpointsBasePath(), name, path, servlet);
	}

	@Override
	public void addServlet(String basePath, String name, String path, HttpServlet servlet) {
		ServletInfo servletInfo = Servlets.servlet(name, servlet.getClass(), new ImmediateInstanceFactory<>(servlet));
		servletInfo.addMapping(path);
		deploymentInfo(normalizeBasePath(basePath)).addServlet(servletInfo);
	}

	@Override
	public void addStaticFileResource(String path, String rootDir, String... welcomeFiles) {
		ResourceHandler resourceHandler = new ResourceHandler(new FileResourceManager(new File(rootDir), 100)) //
				.setWelcomeFiles(welcomeFiles) //
				.setDirectoryListingEnabled(false);

		pathHandler().addPrefixPath(path, resourceHandler);
	}

	@Override
	public void addFilter(FilterSymbol name, Filter filter) {
		addFilter(defaultEndpointsBasePath(), name, filter);
	}

	@Override
	public void addFilter(String basePath, FilterSymbol name, Filter filter) {
		FilterInfo filterInfo = Servlets.filter(name.name(), filter.getClass(), new ImmediateInstanceFactory<>(filter));
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
		if (result == null)
			result = config.getHostName() + ":" + config.getPort();

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
		configureResourceHandlers();
		configureEndpointsHandlers();

		undertowServer().start();

		println( //
				sequence( //
						text("Web Server running. "), //
						cyan("URL:"), //
						text(" http://" + configuration().getHostName() + ":" + configuration().getPort()) //
				) //
		);
	}

	private void configureResourceHandlers() {
		for (StaticFilesystemResourceMapping mapping : webServerConfiguration().getResourceMappings())
			addStaticFileResource( //
					mapping.getPath(), //
					mapping.getRootDir(), //
					mapping.getWelcomeFiles().toArray(new String[0])//
			);
	}

	private void configureEndpointsHandlers() {
		for (String basePath : basePaths())
			try {
				pathHandler().addPrefixPath(basePath, servletDeploymentManager(basePath).start());
			} catch (ServletException e) {
				throw new RuntimeException(e);
			}
	}

	@Managed
	private Undertow undertowServer() {
		WebServerConfiguration configuration = configuration();

		Builder builder = Undertow.builder() //
				.addHttpListener(configuration.getPort(), "0.0.0.0") //
				.setHandler(accessLogHandler());

		SslConfig sslConfig = SslConfig.buildFromConfig(configuration);
		if (sslConfig != null)
			builder.addHttpsListener(sslConfig.port(), "0.0.0.0", sslConfig.sslContext());

		Undertow bean = builder.build();
		return bean;
	}

	@Managed
	private AccessLogHandler accessLogHandler() {
		String logFormat = "%h %l %u \"%r\" %s %b %Dms";
		AccessLogHandler bean = new AccessLogHandler(pathHandler(), logReceiver(), logFormat, Undertow.class.getClassLoader());
		return bean;
	}

	@Managed
	private ReflexAccessLogReceiver logReceiver() {
		ReflexAccessLogReceiver bean = new ReflexAccessLogReceiver();
		return bean;
	}

	@Managed
	private PathHandler pathHandler() {
		PathHandler bean = Handlers.path();
		return bean;
	}

	private StaticWebServerConfiguration webServerConfiguration() {
		return platform.readConfig(StaticWebServerConfiguration.T).get();
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
		return platform.readConfig(WebServerConfiguration.T).get();
	}

	private void registerFilter(DeploymentInfo deploymentInfo, String name, Filter filter, String pathMapping, DispatcherType dispatcherType) {
		FilterInfo filterInfo = Servlets.filter(name, filter.getClass(), new ImmediateInstanceFactory<>(filter));
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
		bean.setApplicationName(platform.applicationName());

		return bean;
	}

	@Managed
	private Set<String> basePaths() {
		return newConcurrentSet();
	}

}