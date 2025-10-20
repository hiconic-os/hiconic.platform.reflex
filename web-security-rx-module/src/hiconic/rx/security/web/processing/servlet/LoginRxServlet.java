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
package hiconic.rx.security.web.processing.servlet;


import org.apache.velocity.VelocityContext;

import com.braintribe.cfg.InitializationAware;

import hiconic.rx.security.web.processing.WebSecurityConstants;
import hiconic.rx.servlet.velocity.BasicTemplateBasedServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginRxServlet extends BasicTemplateBasedServlet implements InitializationAware {

	private static final long serialVersionUID = -3371378397236984055L;
	private static final String signinPageTemplatePath = "templates/login.html.vm";
		
	@Override
	public void postConstruct() {
		setRelativeTemplateLocation(signinPageTemplatePath);
	}

	@Override
	protected VelocityContext createContext(HttpServletRequest request,HttpServletResponse repsonse) {
		VelocityContext context = new VelocityContext();
		context.put("continue", request.getParameter(WebSecurityConstants.REQUEST_PARAM_CONTINUE));
		context.put("message", request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGE));
		context.put("messageStatus", request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGESTATUS));
		// context.put("tribefireRuntime", TribefireRuntime.class);
		
		if (AuthRxServlet.offerStaySigned()) {
			context.put("offerStaySigned", Boolean.TRUE);
		} else {
			context.put("offerStaySigned", Boolean.FALSE);
		}
		
		return context;
	}

}
