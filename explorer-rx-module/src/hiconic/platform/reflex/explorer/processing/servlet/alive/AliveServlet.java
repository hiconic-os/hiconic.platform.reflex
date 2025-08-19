// ============================================================================
package hiconic.platform.reflex.explorer.processing.servlet.alive;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


/**
 * Explorer makes periodic POST requests to services to see if it's alive.  
 */
public class AliveServlet extends HttpServlet {

	private static final long serialVersionUID = -3371378397236984055L;
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.getOutputStream().write("OK".getBytes());
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		super.doGet(req, resp);
	}
}
