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

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;

/** Retains failure information even if response headers already committed a successful status. */
public class AccessLogFailureTrackingHandler implements HttpHandler {

	private static final AttachmentKey<Boolean> failed = AttachmentKey.create(Boolean.class);

	private final HttpHandler next;

	public AccessLogFailureTrackingHandler(HttpHandler next) {
		this.next = next;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		try {
			next.handleRequest(exchange);
		} catch (Exception | Error e) {
			exchange.putAttachment(failed, Boolean.TRUE);
			throw e;
		}
	}

	public static boolean failed(HttpServerExchange exchange) {
		return Boolean.TRUE.equals(exchange.getAttachment(failed));
	}
}
