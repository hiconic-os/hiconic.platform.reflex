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

	public static void throwMethodNotAllowed(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, params);
	}

	public static void throwNotAcceptable(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_NOT_ACCEPTABLE, message, params);
	}

	public static void throwPreConditionFaild(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_PRECONDITION_FAILED, message, params);
	}

	public static void throwExpectationFailed(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_EXPECTATION_FAILED, message, params);
	}

	public static void throwUnauthorized(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_UNAUTHORIZED, message, params);
	}

	public static void throwNotFound(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_NOT_FOUND, message, params);
	}

	public static void throwBadRequest(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_BAD_REQUEST, message, params);
	}

	public static void throwNotImplemented(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_NOT_IMPLEMENTED, message, params);
	}

	public static void throwInternalServerError(String message, Object... params) {
		throwHttpException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message, params);
	}

	public static void throwHttpException(int code, String message, Object... params) {
		throw new HttpException(code, String.format(message, params));
	}

}
