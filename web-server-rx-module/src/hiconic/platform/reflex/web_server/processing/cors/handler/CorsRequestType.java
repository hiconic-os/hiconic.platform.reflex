// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.platform.reflex.web_server.processing.cors.handler;

import jakarta.servlet.http.HttpServletRequest;

public enum CorsRequestType {

	actual, preflight, nonCors;

	public static CorsRequestType get(HttpServletRequest request) {

		String origin = request.getHeader(CorsHeaders.origin.getHeaderName());

		if (origin == null) {
			return nonCors;
		}

		if (isSameOrigin(request, origin)) {
			return nonCors;
		}

		if (isOptions(request) && request.getHeader(CorsHeaders.accessControlRequestMethod.getHeaderName()) != null) {
			return preflight;
		}

		return actual;

	}

	public static boolean isOptions(HttpServletRequest request) {
		return request.getMethod() != null && request.getMethod().equalsIgnoreCase("OPTIONS");
	}

	/**
	 * <p>
	 * Determines whether the {@code Origin} header matches the {@code request} target.
	 * 
	 * <p>
	 * Comparison is based on origin serialization rules defined at http://tools.ietf.org/html/rfc6454#section-6.1
	 * 
	 * @param request
	 *            The base for determining the CORS origin matching target (scheme://host:port)
	 * @param origin
	 *            The {@code Origin} header value
	 * @return Whether the {@code Origin} header matches the target represented by the given {@code request}
	 */
	private static boolean isSameOrigin(HttpServletRequest request, String origin) {

		String scheme = request.getScheme();
		if (scheme == null) {
			return false;
		}

		String host = request.getHeader(CorsHeaders.host.getHeaderName());
		if (host == null) {
			return false;
		}

		StringBuilder target = new StringBuilder(scheme.length() + host.length() + 10);
		target.append(scheme).append("://").append(host);

		int port = request.getServerPort();
		if (("http".equals(scheme) && port != 80 || "https".equals(scheme) && port != 443) && !host.contains(":")) {
			target.append(':').append(port);
		}

		return origin.equalsIgnoreCase(target.toString());

	}

}
