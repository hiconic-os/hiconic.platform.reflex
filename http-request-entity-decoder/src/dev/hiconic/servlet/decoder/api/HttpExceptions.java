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
package dev.hiconic.servlet.decoder.api;

import com.braintribe.exception.HttpException;

import jakarta.servlet.http.HttpServletResponse;

public class HttpExceptions {

	public static void methodNotAllowed(String message, Object... params) {
		httpException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, params);
	}

	public static void notAcceptable(String message, Object... params) {
		httpException(HttpServletResponse.SC_NOT_ACCEPTABLE, message, params);
	}

	public static void preConditionFaild(String message, Object... params) {
		httpException(HttpServletResponse.SC_PRECONDITION_FAILED, message, params);
	}

	public static void expectationFailed(String message, Object... params) {
		httpException(HttpServletResponse.SC_EXPECTATION_FAILED, message, params);
	}

	public static void unauthotized(String message, Object... params) {
		httpException(HttpServletResponse.SC_UNAUTHORIZED, message, params);
	}

	public static void notFound(String message, Object... params) {
		httpException(HttpServletResponse.SC_NOT_FOUND, message, params);
	}

	public static void badRequest(String message, Object... params) {
		httpException(HttpServletResponse.SC_BAD_REQUEST, message, params);
	}

	public static void notImplemented(String message, Object... params) {
		httpException(HttpServletResponse.SC_NOT_IMPLEMENTED, message, params);
	}

	public static void internalServerError(String message, Object... params) {
		httpException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message, params);
	}

	public static void httpException(int code, String message, Object... params) {
		throw new HttpException(code, String.format(message, params));
	}

	/* Source-compatible aliases for code written against the short-lived RX naming. Runtime
	 * consumers use the established names above so applications remain binary-compatible while
	 * the legacy web-api artifact is still present on a migration classpath. */
	@Deprecated public static void throwMethodNotAllowed(String message, Object... params) { methodNotAllowed(message, params); }
	@Deprecated public static void throwNotAcceptable(String message, Object... params) { notAcceptable(message, params); }
	@Deprecated public static void throwPreConditionFaild(String message, Object... params) { preConditionFaild(message, params); }
	@Deprecated public static void throwExpectationFailed(String message, Object... params) { expectationFailed(message, params); }
	@Deprecated public static void throwUnauthorized(String message, Object... params) { unauthotized(message, params); }
	@Deprecated public static void throwNotFound(String message, Object... params) { notFound(message, params); }
	@Deprecated public static void throwBadRequest(String message, Object... params) { badRequest(message, params); }
	@Deprecated public static void throwNotImplemented(String message, Object... params) { notImplemented(message, params); }
	@Deprecated public static void throwInternalServerError(String message, Object... params) { internalServerError(message, params); }
	@Deprecated public static void throwHttpException(int code, String message, Object... params) { httpException(code, message, params); }

}
