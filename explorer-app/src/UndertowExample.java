import java.io.File;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UndertowExample {
    public static void main(String[] args) throws Exception {
        // static file handler
        ResourceHandler resourceHandler = new ResourceHandler(
                new FileResourceManager(new File("src"), 100))
                .setDirectoryListingEnabled(false);

        // define servlet
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(UndertowExample.class.getClassLoader())
                .setContextPath("/my-app/some-servlet")
                .setDeploymentName("myapp.war")
                .addServlets(
						Servlets.servlet("SomeServlet", SomeServlet.class).addMapping("/")
					);

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        HttpHandler servletHandler = manager.start();

        // combine both under /my-app
        PathHandler path = Handlers.path()
        		.addPrefixPath("/my-app/some-servlet", servletHandler) // servlet first
        		.addPrefixPath("/my-app", resourceHandler)            // static files fallback
                ;

        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(path)
                .build();

        server.start();
    }

    public static class SomeServlet extends HttpServlet {
        private static final long serialVersionUID = 1L;

		@Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
            try {
                resp.getWriter().write("Hello from servlet!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}