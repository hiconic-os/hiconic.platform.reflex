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
package hiconic.platform.reflex.web_server.processing;

import java.io.IOException;

import com.braintribe.exception.HttpException;
import com.braintribe.exception.LogPreferences;
import com.braintribe.logging.Logger.LogLevel;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.service.api.result.Unsatisfied;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Rejects requests whose declared body size exceeds the configured limit before
 * a servlet attempts to parse the body. Undertow's native limit remains the hard
 * safety boundary; this filter gives ordinary HTTP clients a modeled response.
 */
public class RequestSizeFilter implements Filter {

	private static final int HTTP_PAYLOAD_TOO_LARGE = 413;

	private Long maxRequestSizeBytes;
	private Long maxMultipartRequestSizeBytes;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest httpRequest)
			validateDeclaredSize(httpRequest);

		chain.doFilter(request, response);
	}

	private void validateDeclaredSize(HttpServletRequest request) {
		long contentLength = request.getContentLengthLong();
		if (contentLength < 0)
			return;

		Long limit = effectiveLimit(request.getContentType());
		if (limit == null || limit < 0 || contentLength <= limit)
			return;

		InvalidArgument reason = InvalidArgument.create(
				"HTTP request body is too large: " + contentLength + " bytes; configured maximum is " + limit + " bytes.");
		HttpException exception = new HttpException(HTTP_PAYLOAD_TOO_LARGE, reason.getText());
		exception.setLogPreferences(new LogPreferences(LogLevel.INFO, false, LogLevel.TRACE));
		exception.withPayload(Unsatisfied.from(Maybe.empty(reason)));
		throw exception;
	}

	private Long effectiveLimit(String contentType) {
		Long limit = enabled(maxRequestSizeBytes);
		if (contentType != null && contentType.regionMatches(true, 0, "multipart/", 0, "multipart/".length())) {
			Long multipartLimit = enabled(maxMultipartRequestSizeBytes);
			if (limit == null || multipartLimit != null && multipartLimit < limit)
				limit = multipartLimit;
		}
		return limit;
	}

	private static Long enabled(Long limit) {
		return limit == null || limit < 0 ? null : limit;
	}

	public void setMaxRequestSizeBytes(Long maxRequestSizeBytes) {
		this.maxRequestSizeBytes = maxRequestSizeBytes;
	}

	public void setMaxMultipartRequestSizeBytes(Long maxMultipartRequestSizeBytes) {
		this.maxMultipartRequestSizeBytes = maxMultipartRequestSizeBytes;
	}
}
