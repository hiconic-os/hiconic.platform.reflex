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
package hiconic.rx.explorer.processing.servlet.alive;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.braintribe.cfg.Required;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Explorer makes periodic POST requests to base services URL to see if the server is running.
 */
public class AliveServlet extends HttpServlet {

	private static final long serialVersionUID = -3371378397236984055L;

	private String homeRelativePath;
	private String notFoundRelativePath;

	@Required
	public void setHomeRelativePath(String homeRelativePath) {
		if (!homeRelativePath.startsWith("/"))
			homeRelativePath = "/" + homeRelativePath;

		this.homeRelativePath = homeRelativePath;
	}

	@Required
	public void setNotFoundRelativePath(String notFoundRelativePath) {
		if (!notFoundRelativePath.startsWith("/"))
			notFoundRelativePath = "/" + notFoundRelativePath;

		this.notFoundRelativePath = notFoundRelativePath;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!isRootRequest(req)) {
			forwardToNotFound(req, resp);
			return;
		}

		resp.setContentType("text/plain");
		resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
		resp.getOutputStream().write("OK".getBytes(StandardCharsets.UTF_8));
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (isRootRequest(req)) {
			resp.sendRedirect(homeRelativePath);
			return;
		}

		forwardToNotFound(req, resp);
	}

	private boolean isRootRequest(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
		String contextRelativePath = requestUri.substring(contextPath.length());
		return contextRelativePath.isEmpty() || "/".equals(contextRelativePath);
	}

	private void forwardToNotFound(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.getRequestDispatcher(notFoundRelativePath).forward(request, response);
	}
}
