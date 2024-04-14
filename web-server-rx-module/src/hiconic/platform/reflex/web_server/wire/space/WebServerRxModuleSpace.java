package hiconic.platform.reflex.web_server.wire.space;

import static com.braintribe.console.ConsoleOutputs.cyan;
import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.platform.reflex.web_server.processing.CallerInfoFilter;
import hiconic.rx.module.api.wire.RxModuleContract;
import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.web.server.api.WebServerContract;
import hiconic.rx.web.server.model.config.StaticFilesystemResourceMapping;
import hiconic.rx.web.server.model.config.StaticWebServerConfiguration;
import hiconic.rx.web.server.model.config.WebServerConfiguration;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;

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
		registerFilter(deploymentInfo(), name, filter);
	}
	
	@Override
	public void addFilterMapping(String filterName, String mapping, DispatcherType dispatcherType) {
		deploymentInfo().addFilterUrlMapping(filterName, mapping, dispatcherType);
	}

	@Override
	public void addFilterServletNameMapping(String filterName, String mapping, DispatcherType dispatcherType) {
		deploymentInfo().addFilterServletNameMapping(filterName, mapping, dispatcherType);
	}
	
	private void registerFilter(DeploymentInfo deploymentInfo, String name, Filter filter) {
		FilterInfo filterInfo = Servlets.filter(name, filter.getClass(), new ImmediateInstanceFactory<>(filter));
		deploymentInfo.addFilter(filterInfo);
		deploymentInfo().addFilter(filterInfo);
	}
	
	@Override
	public String callerInfoFilterName() {
		return "caller-info";
	}
	
	@Managed
	private DeploymentInfo deploymentInfo() {
		DeploymentInfo bean = Servlets.deployment() //
				.setClassLoader(Undertow.class.getClassLoader()) //
				.setContextPath("/") //
				.setDeploymentName("servlet-deployment");
		
		registerFilter(bean, callerInfoFilterName(), callerInfoFilter());
		
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
		
		println(
				sequence(
						text("Web Server running on: "),
						cyan("http://" + configuration().getHostName() + ":"),
						text("" + configuration().getPort())
				)
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
		Undertow bean = Undertow.builder()
				.addHttpListener(configuration.getPort(), "0.0.0.0")
				.setHandler(pathHandler())				      
				.build();
		
		return bean;
	}
	
	private void configureResources(PathHandler path) {
		for (StaticFilesystemResourceMapping mapping: webServerConfiguration().getResourceMappings()) {
			
	        // Create the ResourceHandler for serving static files
	        ResourceHandler resourceHandler = new ResourceHandler(new FileResourceManager(new File(mapping.getRootDir()), 100))
	                .setWelcomeFiles("index.html")
	                .setDirectoryListingEnabled(false);

			path.addPrefixPath(mapping.getPath(), resourceHandler);
		}
	}

}