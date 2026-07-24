package hiconic.rx.log.reflection.processing;

import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import hiconic.rx.log.reflection.model.LogOrigin;
import hiconic.rx.log.reflection.model.LogPosition;
import hiconic.rx.log.reflection.model.LogRecord;
import hiconic.rx.log.reflection.model.LogStreamDescriptor;
import hiconic.rx.log.reflection.model.api.LogRecordPage;
import hiconic.rx.log.reflection.model.api.LogStreams;

/** Deterministic merging and opaque per-node cursor encoding for cluster responses. */
final class LogClusterSupport {
	static final String CURSOR_PREFIX = "cluster-v1:";

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final TypeReference<Map<String, String>> cursorType = new TypeReference<>() {
	};

	private static final Comparator<LogRecord> recordOrder = Comparator
			.comparing(LogRecord::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(record -> applicationId(record.getOrigin()), Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(record -> nodeId(record.getOrigin()), Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparingLong(record -> position(record.getPosition()));

	private static final Comparator<LogStreamDescriptor> streamOrder = Comparator
			.comparing((LogStreamDescriptor stream) -> applicationId(stream.getOrigin()), Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(stream -> nodeId(stream.getOrigin()), Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(LogStreamDescriptor::getDisplayName, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparing(LogStreamDescriptor::getStreamId, Comparator.nullsFirst(Comparator.naturalOrder()));

	private LogClusterSupport() {
	}

	static LogStreams mergeStreams(Map<String, LogStreams> responses, Map<String, String> errors) {
		List<LogStreamDescriptor> streams = responses.values().stream()
				.flatMap(response -> response.getStreams().stream())
				.sorted(streamOrder)
				.toList();

		LogStreams result = LogStreams.T.create();
		result.setStreams(streams);
		result.setErrors(new LinkedHashMap<>(errors));
		return result;
	}

	static LogRecordPage mergePages(Map<String, LogRecordPage> responses, Map<String, String> priorCursors,
			Map<String, String> errors) {
		List<LogRecord> records = responses.values().stream()
				.flatMap(response -> response.getRecords().stream())
				.sorted(recordOrder)
				.toList();

		Map<String, String> nextCursors = new TreeMap<>(priorCursors);
		boolean moreAvailable = false;
		LinkedHashSet<String> observedProperties = new LinkedHashSet<>();
		for (Map.Entry<String, LogRecordPage> entry : responses.entrySet()) {
			LogRecordPage page = entry.getValue();
			if (page.getNextCursor() != null)
				nextCursors.put(entry.getKey(), page.getNextCursor());
			moreAvailable |= page.getMoreAvailable();
			if (page.getObservedProperties() != null)
				observedProperties.addAll(page.getObservedProperties());
		}

		LogRecordPage result = LogRecordPage.T.create();
		result.setRecords(records);
		result.setNextCursor(encodeCursor(nextCursors));
		result.setMoreAvailable(moreAvailable);
		result.setObservedProperties(observedProperties);
		result.setErrors(new LinkedHashMap<>(errors));
		return result;
	}

	static Map<String, String> decodeCursor(String cursor) {
		if (cursor == null || cursor.isBlank())
			return new TreeMap<>();
		if (!cursor.startsWith(CURSOR_PREFIX))
			throw new IllegalArgumentException("A cluster log query requires a cluster cursor");
		try {
			byte[] json = Base64.getUrlDecoder().decode(cursor.substring(CURSOR_PREFIX.length()));
			Map<String, String> decoded = objectMapper.readValue(json, cursorType);
			return new TreeMap<>(decoded);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid cluster log cursor", e);
		}
	}

	static String localCursor(String cursor, String instanceId) {
		if (cursor == null || cursor.isBlank())
			return null;
		if (!cursor.startsWith(CURSOR_PREFIX))
			return cursor;
		return decodeCursor(cursor).get(instanceId);
	}

	private static String encodeCursor(Map<String, String> cursors) {
		try {
			byte[] json = objectMapper.writeValueAsBytes(new TreeMap<>(cursors));
			return CURSOR_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(json);
		} catch (Exception e) {
			throw new IllegalStateException("Could not encode cluster log cursor", e);
		}
	}

	private static String applicationId(LogOrigin origin) {
		return origin == null ? null : origin.getApplicationId();
	}

	private static String nodeId(LogOrigin origin) {
		return origin == null ? null : origin.getNodeId();
	}

	private static long position(LogPosition position) {
		if (position == null)
			return Long.MIN_VALUE;
		if (position.getSequence() != null)
			return position.getSequence();
		if (position.getByteOffset() != null)
			return position.getByteOffset();
		return Long.MIN_VALUE;
	}
}
