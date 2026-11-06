// ============================================================================
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
package hiconic.rx.browser.acceptance.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.junit.Test;

import hiconic.rx.security.web.api.LoginOption;
import hiconic.rx.security.web.api.WebSecurityConstants;
import hiconic.rx.security.web.processing.servlet.LoginRxServlet;
import jakarta.servlet.http.HttpServletRequest;

public class LoginRxServletInterventionTest {

	private static final LoginOption ENTRA = new LoginOption("EntraID", "EntraID", "/auth/entra", "/action/entra", "/entra.png");
	private static final LoginOption OTHER = new LoginOption("Other", "Other", "/auth/other", "/action/other", null);

	@Test
	public void externalInterventionHidesNativeAndAlternativeLoginOptions() {
		VelocityContext context = servlet().context(request(Map.of(
				WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_STATE, "ACTION_REQUIRED",
				WebSecurityConstants.REQUEST_PARAM_LOGIN_OPTION, ENTRA.getId(),
				WebSecurityConstants.REQUEST_PARAM_LOGIN_ACTION, "request-device-browser-approval",
				WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_ACTION_LABEL, "Request approval")));

		assertThat(context.get("externalIntervention")).isEqualTo(true);
		assertThat(context.get("localLoginEnabled")).isEqualTo(false);
		assertThat((List<?>) context.get("loginOptions")).isEmpty();
		assertThat(context.get("interventionActionUrl"))
				.isEqualTo("/action/entra?loginAction=request-device-browser-approval");
	}

	@Test
	public void nativeInterventionKeepsNativeAndExternalLoginOptions() {
		VelocityContext context = servlet().context(request(Map.of(
				WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_STATE, "ACTION_REQUIRED",
				WebSecurityConstants.REQUEST_PARAM_LOGIN_ACTION, "request-device-browser-approval")));

		assertThat(context.get("externalIntervention")).isEqualTo(false);
		assertThat(context.get("localLoginEnabled")).isEqualTo(true);
		assertThat((List<?>) context.get("loginOptions")).hasSize(2);
		assertThat(context.get("interventionActionUrl")).isNull();
	}

	@Test
	public void pendingExternalInterventionRemainsIsolatedWithoutAnAction() {
		VelocityContext context = servlet().context(request(Map.of(
				WebSecurityConstants.REQUEST_PARAM_LOGIN_INTERVENTION_STATE, "PENDING",
				WebSecurityConstants.REQUEST_PARAM_LOGIN_OPTION, ENTRA.getId())));

		assertThat(context.get("externalIntervention")).isEqualTo(true);
		assertThat(context.get("localLoginEnabled")).isEqualTo(false);
		assertThat((List<?>) context.get("loginOptions")).isEmpty();
	}

	private TestLoginRxServlet servlet() {
		TestLoginRxServlet servlet = new TestLoginRxServlet();
		servlet.setLocalLoginEnabled(true);
		servlet.setLoginOptions(List.of(ENTRA, OTHER));
		return servlet;
	}

	private HttpServletRequest request(Map<String, String> parameters) {
		return proxy(HttpServletRequest.class, (method, args) -> {
			if (method.getName().equals("getParameter"))
				return parameters.get(args[0]);
			return defaultValue(method.getReturnType());
		});
	}

	private static class TestLoginRxServlet extends LoginRxServlet {
		private static final long serialVersionUID = 1L;

		VelocityContext context(HttpServletRequest request) {
			return createContext(request, null);
		}
	}

	private interface Invocation {
		Object invoke(Method method, Object[] arguments) throws Throwable;
	}

	@SuppressWarnings("unchecked")
	private <T> T proxy(Class<T> type, Invocation invocation) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
				(proxy, method, args) -> invocation.invoke(method, args));
	}

	private Object defaultValue(Class<?> type) {
		if (!type.isPrimitive())
			return null;
		if (type == boolean.class)
			return false;
		if (type == char.class)
			return '\0';
		return 0;
	}
}
