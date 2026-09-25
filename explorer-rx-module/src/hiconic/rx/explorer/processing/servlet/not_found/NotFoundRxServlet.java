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
package hiconic.rx.explorer.processing.servlet.not_found;

import org.apache.velocity.VelocityContext;

import com.braintribe.cfg.InitializationAware;
import com.braintribe.cfg.Required;

import hiconic.rx.servlet.velocity.BasicTemplateBasedServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class NotFoundRxServlet extends BasicTemplateBasedServlet implements InitializationAware {
	private static final long serialVersionUID = 1L;
	private static final String TEMPLATE_PATH = "templates/not-found.html.vm";

	private String homeRelativePath;

	@Required
	public void setHomeRelativePath(String homeRelativePath) {
		if (!homeRelativePath.startsWith("/"))
			homeRelativePath = "/" + homeRelativePath;

		this.homeRelativePath = homeRelativePath;
	}

	@Override
	public void postConstruct() {
		setRelativeTemplateLocation(TEMPLATE_PATH);
	}

	@Override
	protected VelocityContext createContext(HttpServletRequest request, HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);

		VelocityContext context = new VelocityContext();
		context.put("homeRelativePath", homeRelativePath);
		return context;
	}
}
