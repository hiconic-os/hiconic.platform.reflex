package hiconic.platform.reflex.web_server.processing;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
 * Serves deployment-specific configuration to static web applications.
 * <p>
 * Static web applications are mounted directly on the root {@code PathHandler}; the exact runtime-configuration
 * endpoint therefore deliberately lives at that same Undertow routing level.
 */
public class RuntimeConfigurationHandler implements HttpHandler {
	private final Supplier<? extends Map<String, ?>> properties;

	public RuntimeConfigurationHandler(Supplier<? extends Map<String, ?>> properties) {
		this.properties = properties;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=UTF-8");
		exchange.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-store");
		exchange.getResponseSender().send(toJson(properties.get()), StandardCharsets.UTF_8);
	}

	private static String toJson(Map<String, ?> properties) {
		StringBuilder json = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<String, ?> entry : properties.entrySet()) {
			if (entry.getValue() == null)
				continue;
			if (!first)
				json.append(',');
			json.append('"').append(escape(entry.getKey())).append("\":");
			appendJsonValue(json, entry.getValue());
			first = false;
		}
		return json.append('}').toString();
	}

	private static void appendJsonValue(StringBuilder json, Object value) {
		if (value instanceof Number || value instanceof Boolean) {
			json.append(value);
		} else if (value instanceof Iterable<?> values) {
			json.append('[');
			boolean first = true;
			for (Object element : values) {
				if (!first)
					json.append(',');
				appendJsonValue(json, element);
				first = false;
			}
			json.append(']');
		} else {
			json.append('"').append(escape(String.valueOf(value))).append('"');
		}
	}

	private static String escape(String value) {
		StringBuilder result = new StringBuilder(value.length() + 16);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '\\' -> result.append("\\\\");
				case '"' -> result.append("\\\"");
				case '\b' -> result.append("\\b");
				case '\f' -> result.append("\\f");
				case '\n' -> result.append("\\n");
				case '\r' -> result.append("\\r");
				case '\t' -> result.append("\\t");
				case '<' -> result.append("\\u003c");
				case '>' -> result.append("\\u003e");
				case '&' -> result.append("\\u0026");
				default -> {
					if (c < 0x20)
						result.append(String.format("\\u%04x", (int) c));
					else
						result.append(c);
				}
			}
		}
		return result.toString();
	}
}
