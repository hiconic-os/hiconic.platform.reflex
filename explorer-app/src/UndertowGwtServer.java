import java.io.File;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;

public class UndertowGwtServer {

	public static void main(String[] args) {
		// Path to your GWT output directory
		String webappDir = "web-app/tribefire-explorer"; // Update this to the actual pathInfo

		// Create a deployment for the GWT application
		DeploymentInfo servletBuilder = Servlets.deployment() //
				.setClassLoader(UndertowGwtServer.class.getClassLoader()) //
//				.setContextPath("/tribefire-explorer") // Set the context pathInfo for your app
				.setContextPath("") // Set the context pathInfo for your app
				.setDeploymentName("explorer") //
				//.setResourceManager(new FileResourceManager(new File(webappDir))) //
				.setDefaultEncoding("UTF-8");

		// Create servlet container and deploy
		ServletContainer container = Servlets.defaultContainer();
		DeploymentManager manager = container.addDeployment(servletBuilder);
		manager.deploy();

		PathHandler path = Handlers.path();
		path.addPrefixPath("tribefire-explorer", new ResourceHandler(new FileResourceManager(new File(webappDir), 100))
                .setDirectoryListingEnabled(false)
                .addWelcomeFiles("ClientEntryPoint.html"));
		
		// Start Undertow server
		Undertow server = Undertow.builder() //
				.addHttpListener(8080, "localhost") // Listen on port 8080
				.setHandler(path)
//				.setHandler(Handlers.resource(new PathResourceManager(new File(webappDir).toPath(), 100))
//						.setDirectoryListingEnabled(false)
//						.addWelcomeFiles("ClientEntryPoint.html"))
				//.setHandler(manager.start()) //
				.build();

		server.start();
		System.out.println("Undertow server started at http://localhost:8080/gwtapp");
	}

}