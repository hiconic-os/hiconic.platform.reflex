package hiconic.rx.log.reflection.processing;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.braintribe.model.logging.LogLevel;
import com.braintribe.model.service.api.InstanceId;

import hiconic.rx.log.reflection.model.LogPosition;
import hiconic.rx.log.reflection.model.LogRecord;

/**
 * Compiles the reversible subset of a Logback pattern. Unknown converters are tolerated, but do not become canonical fields.
 */
final class CompiledLogPattern implements CanonicalLogParser {
	private final Pattern recordPattern;
	private final Pattern recordStartPattern;
	private final List<Capture> captures;
	private final InstanceId instanceId;
	private final String streamId;

	private CompiledLogPattern(Pattern recordPattern, Pattern recordStartPattern, List<Capture> captures, InstanceId instanceId,
			String streamId) {
		this.recordPattern = recordPattern;
		this.recordStartPattern = recordStartPattern;
		this.captures = captures;
		this.instanceId = instanceId;
		this.streamId = streamId;
	}

	static CompiledLogPattern compile(String pattern, InstanceId instanceId, String streamId) {
		if (pattern == null || pattern.isBlank())
			return null;

		List<Part> parts = tokenize(pattern);
		StringBuilder full = new StringBuilder("^");
		StringBuilder start = new StringBuilder("^");
		List<Capture> captures = new ArrayList<>();
		boolean startComplete = false;

		for (Part part : parts) {
			if (part.literal != null) {
				String literalRegex = literalRegex(part.literal);
				full.append(literalRegex);
				if (!startComplete)
					start.append(literalRegex);
				continue;
			}

			String word = part.word;
			if ("n".equals(word))
				continue;

			Field field = field(word);
			String valueRegex = valueRegex(field);
			if (field != Field.UNKNOWN) {
				full.append('(').append(valueRegex).append(')');
				captures.add(new Capture(captures.size() + 1, field, part.option));
			} else {
				full.append("(?:").append(valueRegex).append(')');
			}

			if (!startComplete) {
				if (field == Field.MESSAGE || field == Field.THROWABLE) {
					startComplete = true;
				} else {
					start.append("(?:").append(valueRegex).append(')');
				}
			}
		}

		full.append('$');
		Pattern fullPattern = Pattern.compile(full.toString(), Pattern.DOTALL);
		Pattern startPattern = startComplete && start.length() > 1 ? Pattern.compile(start.toString()) : null;
		return new CompiledLogPattern(fullPattern, startPattern, captures, instanceId, streamId);
	}

	static CompiledLogPattern raw(InstanceId instanceId, String streamId) {
		return new CompiledLogPattern(Pattern.compile("(?s)^(.*)$"), null,
				List.of(new Capture(1, Field.MESSAGE, null)), instanceId, streamId);
	}

	@Override
	public boolean startsRecord(String line) {
		return recordStartPattern != null && recordStartPattern.matcher(line).find();
	}

	@Override
	public LogRecord parse(String raw, long byteOffset) {
		Matcher matcher = recordPattern.matcher(raw);
		if (!matcher.matches())
			return rawRecord(raw, byteOffset);

		LogRecord record = baseRecord(raw, byteOffset);
		for (Capture capture : captures) {
			String value = matcher.group(capture.group);
			if (value != null)
				value = value.strip();
			switch (capture.field) {
				case TIMESTAMP -> record.setTimestamp(parseTimestamp(value, capture.option));
				case LEVEL -> record.setLevel(parseLevel(value));
				case LOGGER -> record.setLoggerName(value);
				case THREAD -> record.setThreadName(value);
				case MESSAGE -> record.setMessage(value);
				case THROWABLE -> {
					if (value != null && !value.isBlank())
						record.setThrowable(value);
				}
				case NDC -> record.getProperties().put("ndc", value);
				case MDC -> record.getProperties().put(capture.option == null ? "mdc" : capture.option, value);
				case UNKNOWN -> {
					// Already excluded from captures.
				}
			}
		}
		if (record.getMessage() == null)
			record.setMessage(raw);
		return record;
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

	private static LogLevel parseLevel(String value) {
		if (value == null)
			return null;
		try {
			return LogLevel.valueOf(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static Date parseTimestamp(String value, String option) {
		if (value == null)
			return null;

		String datePattern = option;
		String timeZone = null;
		if (datePattern == null || datePattern.isBlank() || "ISO8601".equalsIgnoreCase(datePattern)) {
			datePattern = "yyyy-MM-dd HH:mm:ss,SSS";
		} else {
			int comma = datePattern.lastIndexOf(',');
			if (comma > 0) {
				String candidate = datePattern.substring(comma + 1).trim();
				if (candidate.matches("[A-Za-z][A-Za-z0-9_+\\-/]*")) {
					timeZone = candidate;
					datePattern = datePattern.substring(0, comma);
				}
			}
		}

		SimpleDateFormat format = new SimpleDateFormat(datePattern, Locale.ROOT);
		format.setLenient(false);
		if (timeZone != null)
			format.setTimeZone(TimeZone.getTimeZone(timeZone));
		try {
			return format.parse(value);
		} catch (IllegalArgumentException | ParseException e) {
			return null;
		}
	}

	private static String valueRegex(Field field) {
		return switch (field) {
			case LEVEL -> "[A-Z]+";
			case LOGGER -> "\\S+";
			case TIMESTAMP, THREAD, MESSAGE, THROWABLE, NDC, MDC, UNKNOWN -> ".*?";
		};
	}

	private static Field field(String word) {
		return switch (word) {
			case "d", "date" -> Field.TIMESTAMP;
			case "p", "le", "level" -> Field.LEVEL;
			case "c", "lo", "logger" -> Field.LOGGER;
			case "t", "thread" -> Field.THREAD;
			case "m", "msg", "message" -> Field.MESSAGE;
			case "ex", "exception", "throwable" -> Field.THROWABLE;
			case "ndc" -> Field.NDC;
			case "X", "mdc" -> Field.MDC;
			default -> Field.UNKNOWN;
		};
	}

	private static List<Part> tokenize(String pattern) {
		List<Part> parts = new ArrayList<>();
		StringBuilder literal = new StringBuilder();
		for (int i = 0; i < pattern.length();) {
			char c = pattern.charAt(i);
			if (c != '%') {
				literal.append(c);
				i++;
				continue;
			}
			if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '%') {
				literal.append('%');
				i += 2;
				continue;
			}
			if (!literal.isEmpty()) {
				parts.add(Part.literal(literal.toString()));
				literal.setLength(0);
			}

			i++;
			while (i < pattern.length() && "-0123456789.".indexOf(pattern.charAt(i)) >= 0)
				i++;
			int wordStart = i;
			while (i < pattern.length() && Character.isLetter(pattern.charAt(i)))
				i++;
			String word = pattern.substring(wordStart, i);
			String option = null;
			if (i < pattern.length() && pattern.charAt(i) == '{') {
				int end = pattern.indexOf('}', i + 1);
				if (end >= 0) {
					option = pattern.substring(i + 1, end);
					i = end + 1;
				}
			}
			parts.add(Part.converter(word, option));
		}
		if (!literal.isEmpty())
			parts.add(Part.literal(literal.toString()));
		return parts;
	}

	private static String literalRegex(String literal) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < literal.length();) {
			if (Character.isWhitespace(literal.charAt(i))) {
				while (i < literal.length() && Character.isWhitespace(literal.charAt(i)))
					i++;
				result.append("\\s+");
			} else {
				int start = i;
				while (i < literal.length() && !Character.isWhitespace(literal.charAt(i)))
					i++;
				result.append(Pattern.quote(literal.substring(start, i)));
			}
		}
		return result.toString();
	}

	private enum Field {
		TIMESTAMP,
		LEVEL,
		LOGGER,
		THREAD,
		MESSAGE,
		THROWABLE,
		NDC,
		MDC,
		UNKNOWN
	}

	private record Capture(int group, Field field, String option) {
	}

	private static final class Part {
		private final String literal;
		private final String word;
		private final String option;

		private Part(String literal, String word, String option) {
			this.literal = literal;
			this.word = word;
			this.option = option;
		}

		private static Part literal(String literal) {
			return new Part(literal, null, null);
		}

		private static Part converter(String word, String option) {
			return new Part(null, word, option);
		}
	}
}
