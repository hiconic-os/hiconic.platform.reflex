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
import java.net.SocketAddress;
import java.util.List;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.web_server.processing.CallerInfoFilter;
import hiconic.platform.reflex.web_server.processing.DefaultRxServlet;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.web.server.model.config.StaticFilesystemResourceMapping;
import hiconic.rx.web.server.model.config.StaticWebServerConfiguration;
import hiconic.rx.web.server.model.config.WebServerConfiguration;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.ListenerInfo;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

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
	
	@Managed
	private DeploymentInfo deploymentInfo() {
		DeploymentInfo bean = Servlets.deployment() //
				.setClassLoader(Undertow.class.getClassLoader()) //
				.setContextPath("/") //
				.setDeploymentName("servlet-deployment");

		addFilter(callerInfoFilterName(), callerInfoFilter());
		addServlet("default-servlet", "/", defaultServlet());

		return bean;
	}

	@Managed
	private DeploymentManager servletDeploymentManager() {
		DeploymentManager manager = Servlets.defaultContainer() //
				.addDeployment(deploymentInfo());
		manager.deploy();

		return manager;
	}

	@Managed
	private CallerInfoFilter callerInfoFilter() {
		return new CallerInfoFilter();
	}

	@Managed
	private DefaultRxServlet defaultServlet() {
		DefaultRxServlet bean = new DefaultRxServlet();
		bean.setApplicationName(platform.applicationName());

		return bean;
	}

	@Managed
	private PathHandler pathHandler() {
		PathHandler bean = Handlers.path();
		configureResources(bean);
		return bean;
	}

	@Override
	public void onApplicationReady() {
		try {
			pathHandler().addPrefixPath("/", servletDeploymentManager().start());
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}

		undertowServer().start();

		println( //
				sequence( //
						text("Web Server running. "), //
						cyan("URL:"), //
						text(" http://" + configuration().getHostName() + ":" + configuration().getPort()) //
				) //
		);
	}

	@Managed
	private WebServerConfiguration configuration() {
		return platform.readConfig(WebServerConfiguration.T).get();
	}

	@Managed
	private StaticWebServerConfiguration webServerConfiguration() {
		return platform.readConfig(StaticWebServerConfiguration.T).get();
	}

	@Managed
	private Undertow undertowServer() {
		WebServerConfiguration configuration = configuration();
		Undertow bean = Undertow.builder() //
				.addHttpListener(configuration.getPort(), "0.0.0.0") //
				.setHandler(pathHandler()) //
				.build();

		return bean;
	}

	private void configureResources(PathHandler path) {
		for (StaticFilesystemResourceMapping mapping : webServerConfiguration().getResourceMappings()) {

			// Create the ResourceHandler for serving static files
			ResourceHandler resourceHandler = new ResourceHandler(new FileResourceManager(new File(mapping.getRootDir()), 100)) //
					.setWelcomeFiles("index.html") //
					.setDirectoryListingEnabled(false);

			path.addPrefixPath(mapping.getPath(), resourceHandler);
		}
	}
	
}