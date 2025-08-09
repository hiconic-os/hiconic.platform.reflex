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

import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.web_server.processing.CallerInfoFilter;
import hiconic.platform.reflex.web_server.processing.DefaultRxServlet;
import hiconic.platform.reflex.web_server.processing.InstanceEndpointConfigurator;
import hiconic.platform.reflex.web_server.processing.ReflexAccessLogReceiver;
import hiconic.platform.reflex.web_server.processing.SslConfig;
import hiconic.platform.reflex.web_server.processing.cors.CorsFilter;
import hiconic.platform.reflex.web_server.processing.cors.handler.BasicCorsHandler;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
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

	@Override
	public void addServlet(String name, String path, HttpServlet servlet) {
		ServletInfo servletInfo = Servlets.servlet(name, servlet.getClass(), new ImmediateInstanceFactory<>(servlet));
		servletInfo.addMapping(path);
		deploymentInfo().addServlet(servletInfo);
	}

	@Override
	public void addFilter(String name, Filter filter) {
		FilterInfo filterInfo = Servlets.filter(name, filter.getClass(), new ImmediateInstanceFactory<>(filter));
		deploymentInfo().addFilter(filterInfo);
	}

	@Override
	public void addFilterMapping(String filterName, String mapping, DispatcherType dispatcherType) {
		deploymentInfo().addFilterUrlMapping(filterName, mapping, dispatcherType);
	}

	@Override
	public void addFilterServletNameMapping(String filterName, String mapping, DispatcherType dispatcherType) {
		deploymentInfo().addFilterServletNameMapping(filterName, mapping, dispatcherType);
	}

	@Override
	public void addEndpoint(String path, Endpoint endpoint) {
		registerWsEndpoint(wsDeploymentInfo(), path, endpoint);
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
	public void onApplicationReady() {

		configureResourceHandlers();
		configureEndpointsHandler();

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
		PathHandler path = pathHandler();

		for (StaticFilesystemResourceMapping mapping : webServerConfiguration().getResourceMappings()) {

			// Create the ResourceHandler for serving static files
			ResourceHandler resourceHandler = new ResourceHandler(new FileResourceManager(new File(mapping.getRootDir()), 100)) //
					.setWelcomeFiles("index.html") //
					.setDirectoryListingEnabled(false);

			path.addPrefixPath(mapping.getPath(), resourceHandler);
		}
	}

	private void configureEndpointsHandler() {
		try {
			pathHandler().addPrefixPath(endpointsBasePath(), servletDeploymentManager().start());
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
	private PathHandler pathHandler() {
		PathHandler bean = Handlers.path();
		return bean;
	}

	private StaticWebServerConfiguration webServerConfiguration() {
		return platform.readConfig(StaticWebServerConfiguration.T).get();
	}

	@Managed
	private DeploymentManager servletDeploymentManager() {
		DeploymentManager manager = Servlets.defaultContainer() //
				.addDeployment(deploymentInfo());
		manager.deploy();

		return manager;
	}

	@Managed
	private DeploymentInfo deploymentInfo() {
		WebServerConfiguration configuration = configuration();

		String endpointsBasePath = endpointsBasePath();

		DeploymentInfo bean = Servlets.deployment() //
				.setClassLoader(Undertow.class.getClassLoader()) //
				.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsDeploymentInfo()).setContextPath(endpointsBasePath) //
				.setDeploymentName("servlet-deployment");

		registerFilter(bean, callerInfoFilterName(), callerInfoFilter(), "/*", DispatcherType.REQUEST);

		if (configuration.getCorsConfiguration() != null)
			registerFilter(bean, "cors", corsFilter(), "/*", DispatcherType.REQUEST);

		return bean;
	}

	private String endpointsBasePath() {
		WebServerConfiguration configuration = configuration();
		String path = configuration.getEndpointsBasePath();
		return path == null ? "/" : "/" + path;
	}

	private WebServerConfiguration configuration() {
		return platform.readConfig(WebServerConfiguration.T).get();
	}

	@Managed
	private CallerInfoFilter callerInfoFilter() {
		return new CallerInfoFilter();
	}

	private void registerFilter(DeploymentInfo deploymentInfo, String name, Filter filter, String pathMapping, DispatcherType dispatcherType) {
		FilterInfo filterInfo = Servlets.filter(name, filter.getClass(), new ImmediateInstanceFactory<>(filter));
		deploymentInfo.addFilter(filterInfo);
		deploymentInfo.addFilterUrlMapping(name, pathMapping, dispatcherType);
	}

	@Managed
	private CorsFilter corsFilter() {
		CorsFilter bean = new CorsFilter();
		bean.setCorsHandler(corsHandler());
		return bean;
	}

	@Managed
	private BasicCorsHandler corsHandler() {
		BasicCorsHandler bean = new BasicCorsHandler();
		bean.setConfiguration(configuration().getCorsConfiguration());
		return bean;
	}

	@Managed
	private WebSocketDeploymentInfo wsDeploymentInfo() {
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

}