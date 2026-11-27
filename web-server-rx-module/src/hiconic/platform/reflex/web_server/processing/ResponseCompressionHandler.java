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

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import hiconic.rx.web.server.model.config.HttpResponseCompression;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.QValueParser;

/** Negotiated transport encoding shared by servlet, RPC and static response paths. */
public final class ResponseCompressionHandler {
    private static final Set<String> DEFAULT_TYPES = Set.of("text/*", "application/json", "application/*+json",
            "application/xml", "application/*+xml", "image/svg+xml", "application/javascript", "application/x-javascript",
            "application/yaml", "application/x-yaml", "gm/*", "multipart/chunked", "multipart/form-data");
    private static final Pattern MIME_PATTERN = Pattern.compile("[a-z0-9!#$&^_.+-]+/(?:[a-z0-9!#$&^_.+-]+|\\*|\\*\\+[a-z0-9!#$&^_.+-]+)");
    private static final AttachmentKey<Boolean> ENCODING_SELECTED = AttachmentKey.create(Boolean.class);

    private ResponseCompressionHandler() {}

    public static HttpHandler wrap(HttpHandler next, HttpResponseCompression configuration) {
        if (configuration != null && Boolean.FALSE.equals(configuration.getEnabled())) return next;
        int minimum = configuration == null || configuration.getMinimumResponseBytes() == null ? 2048 : configuration.getMinimumResponseBytes();
        int level = configuration == null || configuration.getLevel() == null ? 1 : configuration.getLevel();
        if (minimum < 0 || level < 0 || level > 9) throw new IllegalArgumentException("Invalid HTTP response compression size or level");
        Set<String> types = configuration == null || (configuration.getMimeTypes() == null || configuration.getMimeTypes().isEmpty()) ? DEFAULT_TYPES
                : configuration.getMimeTypes().stream().map(type -> { if (type == null) throw new IllegalArgumentException("Null response compression MIME pattern"); return type.toLowerCase(Locale.ROOT); }).collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (types.stream().anyMatch(type -> !MIME_PATTERN.matcher(type).matches())) throw new IllegalArgumentException("Invalid response compression MIME pattern");
        var encodings = new ContentEncodingRepository().addEncodingHandler("gzip", new GzipEncodingProvider(level), 100, exchange -> {
            boolean eligible = eligible(exchange, minimum, types);
            if (eligible) exchange.putAttachment(ENCODING_SELECTED, Boolean.TRUE);
            return eligible;
        });
        var handler = new EncodingHandler(next, encodings);
        return exchange -> {
            exchange.addResponseCommitListener(ResponseCompressionHandler::responseHeaders);
            // Undertow returns null for unsupported codings even if identity was expressly refused.
            if (identityRejected(exchange) && encodings.getContentEncodings(exchange) == null) {
                exchange.setStatusCode(406);
                exchange.endExchange();
                return;
            }
            handler.handleRequest(exchange);
        };
    }

    private static boolean identityRejected(HttpServerExchange exchange) {
        var values = exchange.getRequestHeaders().get(Headers.ACCEPT_ENCODING);
        if (values == null) return false;
        Boolean identity = null, wildcard = null;
        for (var group : QValueParser.parse(values)) for (var value : group) {
            if (value.getValue().equalsIgnoreCase("identity") && identity == null) identity = !value.isQValueZero();
            if (value.getValue().equals("*") && wildcard == null) wildcard = !value.isQValueZero();
        }
        return Boolean.FALSE.equals(identity) || identity == null && Boolean.FALSE.equals(wildcard);
    }

    private static boolean eligible(HttpServerExchange exchange, int minimum, Set<String> types) {
        int status = exchange.getStatusCode();
        if (status < 200 || status == 204 || status == 205 || status == 304 || status == 206
                || exchange.getRequestHeaders().contains(Headers.RANGE) || exchange.getResponseHeaders().contains(Headers.CONTENT_RANGE)
                || exchange.getResponseHeaders().contains(Headers.CONTENT_ENCODING)) return false;
        var cacheControl = exchange.getResponseHeaders().get(Headers.CACHE_CONTROL);
        if (cacheControl != null) for (String header : cacheControl) for (String directive : header.split(","))
            if (directive.trim().equalsIgnoreCase("no-transform")) return false;
        long length = exchange.getResponseContentLength();
        if (length == 0 || length >= 0 && length < minimum) return false;
        String contentType = exchange.getResponseHeaders().getFirst(Headers.CONTENT_TYPE);
        if (contentType == null) return false;
        contentType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (contentType.equals("text/event-stream")) return false;
        for (String type : types) if (matches(type, contentType)) return true;
        return false;
    }

    private static boolean matches(String pattern, String type) {
        int star = pattern.indexOf('*');
        return star < 0 ? pattern.equals(type) : type.startsWith(pattern.substring(0, star)) && type.endsWith(pattern.substring(star + 1));
    }

    private static void responseHeaders(HttpServerExchange exchange) {
        var headers = exchange.getResponseHeaders();
        var vary = headers.get(Headers.VARY);
        boolean present = false;
        if (vary != null) for (String value : vary) for (String token : value.split(","))
            if (token.trim().equals("*") || token.trim().equalsIgnoreCase("Accept-Encoding")) present = true;
        if (!present) headers.add(Headers.VARY, "Accept-Encoding");
        if (Boolean.TRUE.equals(exchange.getAttachment(ENCODING_SELECTED)) && "gzip".equalsIgnoreCase(headers.getFirst(Headers.CONTENT_ENCODING))) {
            String etag = headers.getFirst(Headers.ETAG);
            if (etag != null && etag.startsWith("\"")) headers.put(Headers.ETAG, "W/" + etag);
            if (exchange.getRequestMethod().equals(Methods.HEAD)) headers.remove(Headers.CONTENT_LENGTH);
        }
    }
}
