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
		String interventionAction = request.getParameter(WebSecurityConstants.REQUEST_PARAM_LOGIN_ACTION);
		String loginOptionId = request.getParameter(WebSecurityConstants.REQUEST_PARAM_LOGIN_OPTION);
		String interventionState = request.getParameter(WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_STATE);
		boolean externalIntervention = interventionState != null && loginOptionId != null;
		String interventionActionUrl = actionTargetUrl(loginOptionId, interventionAction, continueUrl);
		if (externalIntervention && interventionActionUrl == null)
			interventionAction = null;
		context.put("continue", continueUrl);
		context.put("message", request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGE));
		context.put("messageStatus", request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGESTATUS));
		context.put("localLoginEnabled", localLoginEnabled && !externalIntervention);
		context.put("loginOptions", externalIntervention ? List.of() : loginOptionViews(continueUrl));
		context.put("externalIntervention", externalIntervention);
		context.put("interventionState", interventionState);
		context.put("interventionTitle", request.getParameter(WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_TITLE));
		context.put("interventionMessage", request.getParameter(WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_MESSAGE));
		context.put("interventionAction", interventionAction);
		context.put("interventionActionLabel", request.getParameter(WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_ACTION_LABEL));
		context.put("interventionActionUrl", interventionActionUrl);
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
				&& request.getParameter(WebSecurityConstants.REQUEST_PARAM_MESSAGESTATUS) == null
				&& request.getParameter(WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_STATE) == null;
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

	private String actionTargetUrl(String loginOptionId, String actionId, String continueUrl) {
		if (loginOptionId == null || actionId == null || actionId.isBlank())
			return null;

		LoginOption option = loginOption(loginOptionId);
		if (option == null || option.getActionTargetPath() == null || option.getActionTargetPath().isBlank())
			return null;

		StringBuilder result = new StringBuilder(option.getActionTargetPath());
		result.append(option.getActionTargetPath().contains("?") ? '&' : '?');
		result.append(WebSecurityConstants.REQUEST_PARAM_LOGIN_ACTION).append('=')
				.append(URLEncoder.encode(actionId, StandardCharsets.UTF_8));
		if (continueUrl != null && !continueUrl.isBlank())
			result.append('&').append(WebSecurityConstants.REQUEST_PARAM_CONTINUE).append('=')
					.append(URLEncoder.encode(continueUrl, StandardCharsets.UTF_8));
		return result.toString();
	}

	private LoginOption loginOption(String loginOptionId) {
		if (loginOptionId == null)
			return null;
		return loginOptions.stream().filter(o -> o.getId().equals(loginOptionId)).findFirst().orElse(null);
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
