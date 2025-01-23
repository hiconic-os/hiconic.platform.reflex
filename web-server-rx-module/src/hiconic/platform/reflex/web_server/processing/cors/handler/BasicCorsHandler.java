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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.braintribe.cfg.Configurable;
import com.braintribe.logging.Logger;

import hiconic.platform.reflex.web_server.processing.cors.exception.CorsException;
import hiconic.platform.reflex.web_server.processing.cors.exception.OriginDeniedException;
import hiconic.platform.reflex.web_server.processing.cors.exception.UnsupportedHeaderException;
import hiconic.platform.reflex.web_server.processing.cors.exception.UnsupportedMethodException;
import hiconic.rx.web.server.model.config.CorsConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BasicCorsHandler implements CorsHandler {

	private CorsConfiguration config;

	private String flattenSupportedHeaders;
	private String flattenSupportedMethods = "";
	private String flattenExposedHeaders = "";

	private String defaultFlattenExposedHeaders;

	private static String separator = ", ";
	
	private String publicServicesUrl;
	
	private Set<String> defaultExposedHeaders = ConcurrentHashMap.newKeySet();

	/**
	 * See http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2 and http://www.w3.org/Protocols/rfc2616/rfc2616-sec2.html#sec2
	 */
	private static final Pattern validHeader = Pattern.compile("^[\\x21-\\x7e&&[^]\\[}{()<>@,;:\\\\\"/?=]]+$");

	private static final Logger log = Logger.getLogger(BasicCorsHandler.class);
	
	@Configurable
	public void setPublicServicesUrl(String publicServicesUrl) {
		this.publicServicesUrl = publicServicesUrl;
	}
	
	@Configurable
	public void addDefaultExposedHeader(String header) {
		this.defaultExposedHeaders.add(header);
	}
	
	@Configurable
	public void addDefaultExposedHeaders(Collection<String> headers) {
		this.defaultExposedHeaders.addAll(headers);
	}

	@Configurable
	public void setConfiguration(CorsConfiguration config) {
		this.config = config;

		if (this.config == null) {

			flattenSupportedHeaders = null;
			flattenSupportedMethods = "";
			flattenExposedHeaders = "";

		} else {

			if (!config.getSupportAnyHeader()) {
				flattenSupportedHeaders = join(config.getSupportedHeaders());
			} else {
				flattenSupportedHeaders = null;
			}

			flattenSupportedMethods = join(config.getSupportedMethods());

			flattenExposedHeaders = join(getDefaultExposedHeaders(), join(config.getExposedHeaders()));

		}

		logCorsConfigurationState();

	}

	@Override
	public void handlePreflight(HttpServletRequest request, HttpServletResponse response) throws CorsException {

		if (config == null) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping preflight CORS request as no configuration is available");
			}
			return;
		}

		String origin = request.getHeader(CorsHeaders.origin.getHeaderName());

		validateOrigin(origin);

		String requestMethodHeader = request.getHeader(CorsHeaders.accessControlRequestMethod.getHeaderName());

		if (requestMethodHeader == null) {
			throw new UnsupportedHeaderException("Missing " + CorsHeaders.accessControlRequestMethod.getHeaderName() + " header");
		}

		requestMethodHeader = requestMethodHeader.toUpperCase();

		String accessControlRequestHeaders = request.getHeader(CorsHeaders.accessControlRequestHeaders.getHeaderName());

		String[] canonizedAccessControlRequestHeaders = canonizeHeaders(accessControlRequestHeaders);

		validateMethod(requestMethodHeader);

		validateAccessControlRequestHeaders(canonizedAccessControlRequestHeaders);

		if (config.getSupportsCredentials()) {
			response.setHeader(CorsHeaders.accessControlAllowCredentials.getHeaderName(), "true");
			setAllowedOrigin(response, origin);
		} else {
			if (config.getAllowAnyOrigin()) {
				setAllowedOrigin(response, "*");
			} else {
				setAllowedOrigin(response, origin);
			}
		}

		if (config.getMaxAge() > 0) {
			response.setHeader(CorsHeaders.accessControlMaxAge.getHeaderName(), Integer.toString(config.getMaxAge()));
		}

		response.addHeader(CorsHeaders.accessControlAllowMethods.getHeaderName(), flattenSupportedMethods);

		if (config.getSupportAnyHeader() && accessControlRequestHeaders != null) {

			response.addHeader(CorsHeaders.accessControlAllowHeaders.getHeaderName(), accessControlRequestHeaders);

		} else if (flattenSupportedHeaders != null && !flattenSupportedHeaders.isEmpty()) {
			response.addHeader(CorsHeaders.accessControlAllowHeaders.getHeaderName(), flattenSupportedHeaders);
		}

	}

	@Override
	public void handleActual(HttpServletRequest request, HttpServletResponse response) throws CorsException {

		if (config == null) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping actual CORS request as no configuration is available");
			}
			return;
		}

		String origin = request.getHeader(CorsHeaders.origin.getHeaderName());

		validateOrigin(origin);

		validateMethod(request.getMethod());

		if (config.getSupportsCredentials()) {
			response.setHeader(CorsHeaders.accessControlAllowCredentials.getHeaderName(), "true");
			setAllowedOrigin(response, origin);
		} else {
			if (config.getAllowAnyOrigin()) {
				setAllowedOrigin(response, "*");
			} else {
				setAllowedOrigin(response, origin);
			}
		}

		if (!flattenExposedHeaders.isEmpty()) {
			response.addHeader(CorsHeaders.accessControlExposeHeaders.getHeaderName(), flattenExposedHeaders);
		}

	}

	protected CorsConfiguration getConfiguration() {
		return config;
	}

	protected void validateOrigin(String origin) throws OriginDeniedException {

		if (config.getAllowAnyOrigin()) {
			if (log.isTraceEnabled()) {
				log.trace("Origin [ " + origin + " ] considered allowed as allowAnyOrigin is set to " + config.getAllowAnyOrigin());
			}
			return;
		}

		if (!config.getAllowedOrigins().contains(origin)) {

			String publicBaseUrl = getPublicServicesBaseUrl();
			if (publicBaseUrl != null && origin.equalsIgnoreCase(publicBaseUrl)) {
				log.trace(() -> "The origin [ " + origin + " ] matches the public services base URL [ " + publicBaseUrl + " ]. Granting access.");
			} else {

				if (log.isWarnEnabled()) {
					if (log.isDebugEnabled()) {
						log.debug("allowAnyOrigin is disabled and the origin [ " + origin + " ] is not one of the allowed origins: "
								+ config.getAllowedOrigins());
					}
					log.warn("Denied access from origin [ " + origin + " ]");
				}
				throw new OriginDeniedException("Origin [ " + origin + " ] is not allowed");

			}
		}

		if (log.isTraceEnabled()) {
			log.trace("Origin [ " + origin + " ] considered allowed");
		}

	}

	private String getPublicServicesBaseUrl() {
		return publicServicesUrl;
	}

	protected void validateMethod(String method) throws UnsupportedMethodException {

		if (method == null) {
			throw new UnsupportedMethodException("null HTTP method is not supported");
		}

		if (config.getSupportedMethods() == null || !config.getSupportedMethods().contains(method)) {
			if (log.isWarnEnabled()) {
				if (log.isDebugEnabled()) {
					log.debug("The HTTP method [ " + method + " ] is not one of the supported methods: " + config.getSupportedMethods());
				}
				log.warn("Unsupported HTTP method [ " + method + " ] detected");
			}
			throw new UnsupportedMethodException("HTTP method [ " + method + " ] is not supported");
		}

		if (log.isTraceEnabled()) {
			log.trace("HTTP method [ " + method + " ] considered supported");
		}

	}

	protected void validateAccessControlRequestHeaders(String[] accessControlRequestHeaders) throws UnsupportedHeaderException {

		if (accessControlRequestHeaders == null || accessControlRequestHeaders.length == 0) {
			if (log.isTraceEnabled()) {
				log.trace("No Access-Control-Request-Header to be validated");
			}
			return;
		}

		if (config.getSupportAnyHeader()) {
			if (log.isTraceEnabled()) {
				log.trace("Access-Control-Request-Header(s) [ " + join(accessControlRequestHeaders)
						+ " ] considered allowed as supportAnyHeader is set to " + config.getSupportAnyHeader());
			}
			return;
		}

		if (config.getSupportedHeaders() == null || config.getSupportedHeaders().isEmpty()) {
			throw new UnsupportedHeaderException("Noone of the given Access-Control-Request-Header(s) [ " + join(accessControlRequestHeaders)
					+ " ] are valid, no header is supported");
		}

		for (String accessControlRequestHeader : accessControlRequestHeaders) {
			if (!config.getSupportedHeaders().contains(accessControlRequestHeader)) {
				throw new UnsupportedHeaderException("Access-Control-Request-Header [ " + accessControlRequestHeader + " ] is not supported");
			}
		}

	}

	protected void setAllowedOrigin(HttpServletResponse response, String origin) {

		response.addHeader(CorsHeaders.accessControlAllowOrigin.getHeaderName(), origin);

		if (!origin.equals("*")) {
			response.addHeader(CorsHeaders.vary.getHeaderName(), "Origin");
		}

	}

	protected String getDefaultExposedHeaders() {

		if (defaultFlattenExposedHeaders != null) {
			return defaultFlattenExposedHeaders;
		}

		this.defaultFlattenExposedHeaders = join(defaultExposedHeaders.toArray(String[]::new));

		return defaultFlattenExposedHeaders;

	}

	protected void logCorsConfigurationState() {

		if (!log.isInfoEnabled()) {
			return;
		}

		if (this.config == null) {
			log.info("Initialized CORS Handler with no configuration. CORS policy will not be enforced.");
			return;
		}

		String nl = "\n";

		StringBuilder sb = new StringBuilder();

		sb.append("Initialized CORS Handler:").append(nl);

		sb.append(" - Allow Any Origin: ").append(config.getAllowAnyOrigin()).append(nl);

		sb.append(" - Allowed Origins: ");
		if (config.getAllowedOrigins() != null && !config.getAllowedOrigins().isEmpty()) {
			sb.append(join(config.getAllowedOrigins()));
		} else {
			sb.append("<not provided>");
		}
		sb.append(nl);

		sb.append(" - Support Credentials: ").append(config.getSupportsCredentials()).append(nl);
		sb.append(" - Max age: ").append(config.getMaxAge()).append(nl);
		sb.append(" - Support Any Header: ").append(config.getSupportAnyHeader()).append(nl);

		sb.append(" - Supported Headers: ");
		if (flattenSupportedHeaders != null && !flattenSupportedHeaders.isEmpty()) {
			sb.append(flattenSupportedHeaders);
		} else {
			sb.append("<not provided>");
		}
		sb.append(nl);

		sb.append(" - Supported Methods: ");
		if (flattenSupportedMethods != null && !flattenSupportedMethods.isEmpty()) {
			sb.append(flattenSupportedMethods);
		} else {
			sb.append("<not provided>");
		}
		sb.append(nl);

		sb.append(" - Exposed Headers: ");
		if (flattenExposedHeaders != null && !flattenExposedHeaders.isEmpty()) {
			sb.append(flattenExposedHeaders);
		} else {
			sb.append("<not provided>");
		}

		log.info(sb.toString());

	}

	protected static String join(String[] array) {
		return join(array, separator);
	}

	@SuppressWarnings("hiding")
	protected static String join(String[] array, String separator) {
		if (array == null || array.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			sb.append(array[i]);
			if (i != (array.length - 1)) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	protected static String join(Collection<String> collection) {
		return join(collection, separator);
	}

	@SuppressWarnings("hiding")
	protected static String join(Collection<String> collection, String separator) {
		if (collection == null || collection.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		Iterator<String> it = collection.iterator();
		while (it.hasNext()) {
			sb.append(it.next());
			if (it.hasNext()) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	protected static String join(String a, String b) {

		if (a == null || a.trim().isEmpty()) {
			return b;
		}

		if (b == null || b.trim().isEmpty()) {
			return a;
		}

		return a + separator + b;

	}

	protected static String[] splitHeader(String headerValue) {

		if (headerValue == null) {
			return new String[0];
		}

		headerValue = headerValue.trim();

		if (headerValue.isEmpty()) {
			return new String[0];
		}

		return headerValue.split("\\s*,\\s*|\\s+");
	}

	protected static String[] canonizeHeaders(String accessControlRequestHeaders) throws UnsupportedHeaderException {

		String[] accessControlRequestHeadersValues = splitHeader(accessControlRequestHeaders);

		String[] requestHeaders = new String[accessControlRequestHeadersValues.length];

		for (int i = 0; i < requestHeaders.length; i++) {
			requestHeaders[i] = canonizeHeaderName(accessControlRequestHeadersValues[i]);
		}

		return requestHeaders;

	}

	protected static String canonizeHeaderName(String headerName) throws UnsupportedHeaderException {

		headerName = headerName != null ? headerName.trim() : "";

		if (headerName.isEmpty()) {
			throw new UnsupportedHeaderException("Bad request header");
		}

		if (!validHeader.matcher(headerName).matches()) {
			throw new UnsupportedHeaderException("Invalid header field name [ " + headerName + " ] syntax (see RFC 2616)");
		}

		String[] nameParts = headerName.toLowerCase().split("-");

		String canonizedHeaderName = "";

		for (int i = 0; i < nameParts.length; i++) {
			char[] c = nameParts[i].toCharArray();
			c[0] = Character.toUpperCase(c[0]);
			if (i >= 1) {
				canonizedHeaderName = canonizedHeaderName + "-";
			}
			canonizedHeaderName = canonizedHeaderName + String.valueOf(c);
		}

		return canonizedHeaderName;
	}

}
