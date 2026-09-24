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

import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.AuthenticationFailure;
import com.braintribe.gm.model.security.reason.Forbidden;

import hiconic.rx.security.web.processing.servlet.UiSecurityFailureResponse;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UiSecurityFailureResponseTest {

	private static final String ACCESS_DENIED_PATH = "/_rx/security/access-denied";

	@Test
	public void unauthenticatedUiRequestRedirectsToLogin() throws Exception {
		Map<String, Object> requestCalls = new HashMap<>();
		Map<String, Object> responseCalls = new HashMap<>();
		HttpServletRequest request = request(requestCalls, null);
		HttpServletResponse response = response(responseCalls);

		new UiSecurityFailureResponse("/login", ACCESS_DENIED_PATH).respond(request, response,
				Reasons.build(AuthenticationFailure.T).text("Authentication required").toReason());

		assertThat(responseCalls.get("redirect"))
				.isEqualTo("http://example.test/services/login?continue=http%3A%2F%2Fexample.test%2Fservices%2Fabout%3Fdetails%3Dfull");
		assertThat(requestCalls).doesNotContainKey("forward");
	}

	@Test
	public void forbiddenUiRequestIsForwardedWithoutChangingItsUrl() throws Exception {
		Map<String, Object> requestCalls = new HashMap<>();
		Map<String, Object> responseCalls = new HashMap<>();
		RequestDispatcher dispatcher = proxy(RequestDispatcher.class, (method, args) -> {
			if (method.getName().equals("forward"))
				requestCalls.put("forward", true);
			return null;
		});
		HttpServletRequest request = request(requestCalls, dispatcher);
		HttpServletResponse response = response(responseCalls);

		new UiSecurityFailureResponse("/login", ACCESS_DENIED_PATH).respond(request, response,
				Reasons.build(Forbidden.T).text("Insufficient privileges").toReason());

		assertThat(requestCalls.get("dispatcherPath")).isEqualTo(ACCESS_DENIED_PATH);
		assertThat(requestCalls.get("forward")).isEqualTo(true);
		assertThat(responseCalls.get("status")).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
		assertThat(responseCalls).doesNotContainKey("redirect");
	}

	private HttpServletRequest request(Map<String, Object> calls, RequestDispatcher dispatcher) {
		return proxy(HttpServletRequest.class, (method, args) -> switch (method.getName()) {
			case "getScheme" -> "http";
			case "getServerName" -> "example.test";
			case "getServerPort" -> 80;
			case "getContextPath" -> "/services";
			case "getServletPath" -> "/about";
			case "getPathInfo" -> null;
			case "getQueryString" -> "details=full";
			case "getRequestDispatcher" -> {
				calls.put("dispatcherPath", args[0]);
				yield dispatcher;
			}
			default -> defaultValue(method.getReturnType());
		});
	}

	private HttpServletResponse response(Map<String, Object> calls) {
		return proxy(HttpServletResponse.class, (method, args) -> {
			if (method.getName().equals("sendRedirect"))
				calls.put("redirect", args[0]);
			else if (method.getName().equals("setStatus"))
				calls.put("status", args[0]);
			return defaultValue(method.getReturnType());
		});
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
