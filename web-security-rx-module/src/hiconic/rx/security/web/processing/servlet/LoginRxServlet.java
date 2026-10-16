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


import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.InitializationAware;

import hiconic.rx.security.web.api.LoginOption;
import hiconic.rx.security.web.api.WebSecurityConstants;
import hiconic.rx.servlet.velocity.BasicTemplateBasedServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginRxServlet extends BasicTemplateBasedServlet implements InitializationAware {

	private static final long serialVersionUID = -3371378397236984055L;
	private static final String signinPageTemplatePath = "templates/login.html.vm";

	private boolean localLoginEnabled = true;
	private List<LoginOption> loginOptions = List.of();
		
	@Override
	public void postConstruct() {
		setRelativeTemplateLocation(signinPageTemplatePath);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (shouldRedirectDirectly(request)) {
			response.sendRedirect(targetUrl(loginOptions.get(0), request.getParameter(WebSecurityConstants.REQUEST_PARAM_CONTINUE)));
			return;
		}

		super.service(request, response);
	}

	@Override
	protected VelocityContext createContext(HttpServletRequest request,HttpServletResponse repsonse) {
		VelocityContext context = new VelocityContext();
		String continueUrl = request.getParameter(WebSecurityConstants.REQUEST_PARAM_CONTINUE);
		context.put("continue", continueUrl);
		context.put("message", request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGE));
		context.put("messageStatus", request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGESTATUS));
		context.put("localLoginEnabled", localLoginEnabled);
		context.put("loginOptions", loginOptionViews(continueUrl));
		// context.put("tribefireRuntime", TribefireRuntime.class);
		
		if (AuthRxServlet.offerStaySigned()) {
			context.put("offerStaySigned", Boolean.TRUE);
		} else {
			context.put("offerStaySigned", Boolean.FALSE);
		}
		
		return context;
	}

	private boolean shouldRedirectDirectly(HttpServletRequest request) {
		return !localLoginEnabled && loginOptions.size() == 1
				&& request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGE) == null
				&& request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGESTATUS) == null;
	}

	private List<Map<String, String>> loginOptionViews(String continueUrl) {
		List<Map<String, String>> result = new ArrayList<>(loginOptions.size());
		for (LoginOption option : loginOptions) {
			Map<String, String> view = new LinkedHashMap<>();
			view.put("id", option.getId());
			view.put("label", option.getLabel());
			view.put("url", targetUrl(option, continueUrl));
			view.put("iconPath", option.getIconPath());
			result.add(view);
		}
		return result;
	}

	private String targetUrl(LoginOption option, String continueUrl) {
		if (continueUrl == null || continueUrl.isBlank())
			return option.getTargetPath();

		String separator = option.getTargetPath().contains("?") ? "&" : "?";
		return option.getTargetPath() + separator + WebSecurityConstants.REQUEST_PARAM_CONTINUE + "="
				+ URLEncoder.encode(continueUrl, StandardCharsets.UTF_8);
	}

	@Configurable
	public void setLocalLoginEnabled(boolean localLoginEnabled) {
		this.localLoginEnabled = localLoginEnabled;
	}

	@Configurable
	public void setLoginOptions(List<LoginOption> loginOptions) {
		this.loginOptions = List.copyOf(loginOptions);
	}

}
