+// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.security.web.processing.servlet;

import org.apache.velocity.VelocityContext;

import com.braintribe.cfg.InitializationAware;

import hiconic.rx.servlet.velocity.BasicTemplateBasedServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AccessDeniedRxServlet extends BasicTemplateBasedServlet implements InitializationAware {
	private static final long serialVersionUID = 1L;
	private static final String TEMPLATE_PATH = "templates/access-denied.html.vm";

	@Override
	public void postConstruct() {
		setRelativeTemplateLocation(TEMPLATE_PATH);
	}

	@Override
	protected VelocityContext createContext(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		return new VelocityContext();
	}
}
