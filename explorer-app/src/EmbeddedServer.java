import java.io.File;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import jakarta.servlet.ServletException;

public class EmbeddedServer {

    public static void main(final String[] args) throws ServletException {

        // The pathInfo to your compiled GWT output directory
        String webAppPath = "web-app";

        // Create a FileResourceManager to serve static files
        File webAppDir = new File(webAppPath);
        FileResourceManager resourceManager = new FileResourceManager(webAppDir, 1024);

        // Create the DeploymentInfo object
        DeploymentInfo servletBuilder = new DeploymentInfo()
                .setClassLoader(EmbeddedServer.class.getClassLoader())
                .setContextPath("/tribefire-explorer") // The context pathInfo for your app
                .setDeploymentName("explorer.war")
                .setResourceManager(resourceManager)
                .addWelcomePage("ClientEntryPoint.html");

        // Get the ServletContainer and create a DeploymentManager
        ServletContainer servletContainer = ServletContainer.Factory.newInstance();
        DeploymentManager manager = servletContainer.addDeployment(servletBuilder);
        manager.deploy();

        // Create and start the Undertow server
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(manager.start())
                .build();
        server.start();

        System.out.println("Undertow server started on http://localhost:8080/explorer");
    }
}