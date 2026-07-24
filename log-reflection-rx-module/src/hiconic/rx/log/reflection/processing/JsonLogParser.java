package hiconic.rx.log.reflection.processing;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.braintribe.model.logging.LogLevel;
import com.braintribe.model.service.api.InstanceId;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import hiconic.rx.log.reflection.model.LogPosition;
import hiconic.rx.log.reflection.model.LogRecord;
import hiconic.rx.log.reflection.model.LogSourceLocation;

/** Canonicalizes the common Logstash/Logback JSON field conventions while retaining unknown fields as properties. */
final class JsonLogParser implements CanonicalLogParser {
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final Set<String> canonicalFields = Set.of(
			"@timestamp", "timestamp", "timeStamp",
			"level", "level_name",
			"logger", "logger_name", "loggerName",
			"thread", "thread_name", "threadName",
			"message", "formattedMessage",
			"stack_trace", "throwable", "exception",
			"class", "class_name", "method", "method_name", "file", "file_name", "line", "line_number",
			"mdc");

	private final InstanceId instanceId;
	private final String streamId;

	JsonLogParser(InstanceId instanceId, String streamId) {
		this.instanceId = instanceId;
		this.streamId = streamId;
	}

	@Override
	public boolean startsRecord(String line) {
		return line.stripLeading().startsWith("{");
	}

	@Override
	public LogRecord parse(String raw, long byteOffset) {
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root == null || !root.isObject())
				return rawRecord(raw, byteOffset);

			LogRecord record = baseRecord(raw, byteOffset);
			record.setTimestamp(timestamp(first(root, "@timestamp", "timestamp", "timeStamp")));
			record.setLevel(level(text(root, "level", "level_name")));
			record.setLoggerName(text(root, "logger_name", "logger", "loggerName"));
			record.setThreadName(text(root, "thread_name", "thread", "threadName"));
			record.setMessage(text(root, "message", "formattedMessage"));
			record.setThrowable(text(root, "stack_trace", "throwable", "exception"));
			record.setSourceLocation(sourceLocation(root));
			collectProperties(root, record.getProperties());
			if (record.getMessage() == null)
				record.setMessage(raw);
			return record;
		} catch (Exception e) {
			return rawRecord(raw, byteOffset);
		}
	}

	@Override
	public LogRecord rawRecord(String raw, long byteOffset) {
		LogRecord record = baseRecord(raw, byteOffset);
		record.setMessage(raw);
		return record;
	}

	private LogRecord baseRecord(String raw, long byteOffset) {
		LogRecord record = LogRecord.T.create();
		record.setOrigin(LogReflectionModelTools.origin(instanceId));
		record.setStreamId(streamId);
		record.setRawText(raw);
		record.setProperties(new LinkedHashMap<>());
		LogPosition position = LogPosition.T.create();
		position.setByteOffset(byteOffset);
		record.setPosition(position);
		return record;
	}

	private static void collectProperties(JsonNode root, Map<String, String> target) {
		JsonNode mdc = root.get("mdc");
		if (mdc != null && mdc.isObject())
			mdc.fields().forEachRemaining(entry -> target.put(entry.getKey(), value(entry.getValue())));

		Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
		while (fields.hasNext()) {
			Map.Entry<String, JsonNode> entry = fields.next();
			if (!canonicalFields.contains(entry.getKey()))
				target.put(entry.getKey(), value(entry.getValue()));
		}
	}

	private static LogSourceLocation sourceLocation(JsonNode root) {
		String className = text(root, "class_name", "class");
		String methodName = text(root, "method_name", "method");
		String fileName = text(root, "file_name", "file");
		Integer lineNumber = integer(root, "line_number", "line");
		if (className == null && methodName == null && fileName == null && lineNumber == null)
			return null;

		LogSourceLocation source = LogSourceLocation.T.create();
		source.setClassName(className);
		source.setMethodName(methodName);
		source.setFileName(fileName);
		source.setLineNumber(lineNumber);
		return source;
	}

	private static Date timestamp(JsonNode node) {
		if (node == null || node.isNull())
			return null;
		if (node.isNumber())
			return new Date(node.longValue());
		String value = node.asText();
		try {
			return Date.from(Instant.parse(value));
		} catch (DateTimeParseException e) {
			try {
				return Date.from(OffsetDateTime.parse(value).toInstant());
			} catch (DateTimeParseException ignored) {
				return null;
			}
		}
	}

	private static LogLevel level(String value) {
		if (value == null)
			return null;
		try {
			return LogLevel.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static String text(JsonNode root, String... names) {
		JsonNode node = first(root, names);
		return node == null || node.isNull() ? null : value(node);
	}

	private static Integer integer(JsonNode root, String... names) {
		JsonNode node = first(root, names);
		if (node == null || node.isNull())
			return null;
		if (node.canConvertToInt())
			return node.intValue();
		try {
			return Integer.valueOf(node.asText());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static JsonNode first(JsonNode root, String... names) {
		for (String name : names) {
			JsonNode node = root.get(name);
			if (node != null)
				return node;
		}
		return null;
	}

	private static String value(JsonNode node) {
		return node.isValueNode() ? node.asText() : node.toString();
	}
}
