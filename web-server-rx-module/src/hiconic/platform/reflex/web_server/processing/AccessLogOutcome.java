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

import io.undertow.attribute.ReadOnlyAttributeException;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ExceptionHandler;
import io.undertow.servlet.attribute.ServletRequestAttribute;
import jakarta.servlet.ServletRequest;

/** Supplies the access-log outcome token, including traceback correlation for handled failures. */
public final class AccessLogOutcome {

	public static final String requestAttribute = "hiconic.accessLogOutcome";

	private AccessLogOutcome() {
	}

	public static void recordFailure(ServletRequest request, String tracebackId) {
		request.setAttribute(requestAttribute, "FAILED[" + tracebackId + "]");
	}

	public static void complete(HttpServerExchange exchange) {
		ServletRequestAttribute outcome = new ServletRequestAttribute(requestAttribute);
		String failure = outcome.readAttribute(exchange);
		if (failure != null) {
			write(outcome, exchange, prefixStatus(exchange, failure));
		} else if (AccessLogFailureTrackingHandler.failed(exchange) || exchange.getAttachment(ExceptionHandler.THROWABLE) != null) {
			write(outcome, exchange, prefixStatus(exchange, "FAILED"));
		} else if (exchange.isResponseStarted()) {
			write(outcome, exchange, Integer.toString(exchange.getStatusCode()));
		} else {
			write(outcome, exchange, "NO_RESPONSE");
		}
	}

	private static void write(ServletRequestAttribute outcome, HttpServerExchange exchange, String value) {
		try {
			outcome.writeAttribute(exchange, value);
		} catch (ReadOnlyAttributeException e) {
			throw new IllegalStateException("Could not record access-log outcome", e);
		}
	}

	private static String prefixStatus(HttpServerExchange exchange, String failure) {
		return exchange.isResponseStarted() ? exchange.getStatusCode() + "/" + failure : failure;
	}
}
