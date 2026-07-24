package hiconic.rx.log.reflection.processing;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogPropertyFilter;
import hiconic.rx.log.reflection.model.LogRecord;

final class LogRecordFiltering {
	private LogRecordFiltering() {
	}

	static boolean matches(LogRecord record, LogFilter filter) {
		if (filter == null)
			return true;
		if (filter.getFrom() != null && (record.getTimestamp() == null || record.getTimestamp().before(filter.getFrom())))
			return false;
		if (filter.getTo() != null && (record.getTimestamp() == null || record.getTimestamp().after(filter.getTo())))
			return false;
		if (filter.getLevels() != null && !filter.getLevels().isEmpty() && !filter.getLevels().contains(record.getLevel()))
			return false;
		if (!matchesPrefix(record.getLoggerName(), filter.getLoggerNames()))
			return false;
		if (!containsIgnoreCase(record.getLoggerName(), filter.getLoggerNameContains()))
			return false;
		if (!matchesExact(record.getThreadName(), filter.getThreadNames()))
			return false;
		if (!containsFulltext(record, filter.getFulltext()))
			return false;
		if (filter.getPropertyFilters() != null) {
			for (LogPropertyFilter propertyFilter : filter.getPropertyFilters()) {
				String actual = record.getProperties().get(propertyFilter.getName());
				if (actual == null || !actual.equals(propertyFilter.getValue()))
					return false;
			}
		}
		return true;
	}

	static Set<String> observedProperties(List<LogRecord> records) {
		Set<String> result = new HashSet<>();
		for (LogRecord record : records)
			result.addAll(record.getProperties().keySet());
		return result;
	}

	private static boolean matchesPrefix(String actual, Set<String> expectedPrefixes) {
		if (expectedPrefixes == null || expectedPrefixes.isEmpty())
			return true;
		for (String prefix : expectedPrefixes)
			if (actual != null && actual.startsWith(prefix))
				return true;
		return false;
	}

	private static boolean matchesExact(String actual, Set<String> expected) {
		return expected == null || expected.isEmpty() || expected.contains(actual);
	}

	private static boolean containsFulltext(LogRecord record, String fulltext) {
		if (fulltext == null || fulltext.isBlank())
			return true;
		String needle = fulltext.toLowerCase(Locale.ROOT);
		return contains(record.getMessage(), needle) || contains(record.getThrowable(), needle) || contains(record.getRawText(), needle)
				|| contains(record.getLoggerName(), needle);
	}

	private static boolean containsIgnoreCase(String value, String query) {
		return query == null || query.isBlank() || contains(value, query.toLowerCase(Locale.ROOT));
	}

	private static boolean contains(String value, String lowerCaseNeedle) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(lowerCaseNeedle);
	}
}
