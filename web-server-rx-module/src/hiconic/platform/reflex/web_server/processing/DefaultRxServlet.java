package hiconic.platform.reflex.web_server.processing;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Just a "smoke-test" Servlet, informing the user the web app is running.
 */
public class DefaultRxServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private String applicationName;

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String requestURI = req.getRequestURI();
		if ("/".equals(requestURI))
			resp.getWriter().write("Application '" + applicationName + "' is running.");
		else
			super.doGet(req, resp);
	}

}
