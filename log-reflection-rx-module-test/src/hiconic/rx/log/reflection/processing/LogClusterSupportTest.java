package hiconic.rx.log.reflection.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import hiconic.rx.log.reflection.model.LogOrigin;
import hiconic.rx.log.reflection.model.LogPosition;
import hiconic.rx.log.reflection.model.LogRecord;
import hiconic.rx.log.reflection.model.LogStreamDescriptor;
import hiconic.rx.log.reflection.model.api.LogRecordPage;
import hiconic.rx.log.reflection.model.api.LogStreams;

public class LogClusterSupportTest {
	@Test
	public void mergesRecordsChronologicallyAndCarriesIndependentNodeCursors() {
		LogRecordPage node1 = page(record("app", "node-1", 200, 2, "later"), "cursor-1", false, "tenant");
		LogRecordPage node2 = page(record("app", "node-2", 100, 7, "earlier"), "cursor-2", true, "requestId");

		Map<String, LogRecordPage> responses = new LinkedHashMap<>();
		responses.put("app@node-1", node1);
		responses.put("app@node-2", node2);
		Map<String, String> previous = Map.of("app@offline", "cursor-old");
		Map<String, String> errors = Map.of("app@node-3", "timeout");

		LogRecordPage merged = LogClusterSupport.mergePages(responses, previous, errors);

		assertThat(merged.getRecords()).extracting(LogRecord::getMessage).containsExactly("earlier", "later");
		assertThat(merged.getObservedProperties()).containsExactlyInAnyOrder("tenant", "requestId");
		assertThat(merged.getMoreAvailable()).isTrue();
		assertThat(merged.getErrors()).containsEntry("app@node-3", "timeout");

		Map<String, String> cursors = LogClusterSupport.decodeCursor(merged.getNextCursor());
		assertThat(cursors).containsEntry("app@node-1", "cursor-1")
				.containsEntry("app@node-2", "cursor-2")
				.containsEntry("app@offline", "cursor-old");
		assertThat(LogClusterSupport.localCursor(merged.getNextCursor(), "app@node-2")).isEqualTo("cursor-2");
	}

	@Test
	public void ordersStreamInventoryStablyByOriginAndName() {
		LogStreams first = streams(stream("app", "node-2", "Warnings", "warn"), stream("app", "node-1", "Main", "main"));
		LogStreams second = streams(stream("app", "node-1", "Audit", "audit"));

		LogStreams merged = LogClusterSupport.mergeStreams(
				Map.of("app@node-2", first, "app@node-1", second),
				Map.of());

		assertThat(merged.getStreams()).extracting(LogStreamDescriptor::getStreamId)
				.containsExactly("audit", "main", "warn");
	}

	private static LogRecordPage page(LogRecord record, String cursor, boolean more, String property) {
		LogRecordPage page = LogRecordPage.T.create();
		page.setRecords(List.of(record));
		page.setNextCursor(cursor);
		page.setMoreAvailable(more);
		page.setObservedProperties(Set.of(property));
		page.setErrors(new LinkedHashMap<>());
		return page;
	}

	private static LogRecord record(String applicationId, String nodeId, long timestamp, long sequence, String message) {
		LogRecord record = LogRecord.T.create();
		record.setOrigin(origin(applicationId, nodeId));
		record.setTimestamp(new Date(timestamp));
		record.setMessage(message);
		LogPosition position = LogPosition.T.create();
		position.setSequence(sequence);
		record.setPosition(position);
		return record;
	}

	private static LogStreams streams(LogStreamDescriptor... descriptors) {
		LogStreams streams = LogStreams.T.create();
		streams.setStreams(List.of(descriptors));
		streams.setErrors(new LinkedHashMap<>());
		return streams;
	}

	private static LogStreamDescriptor stream(String applicationId, String nodeId, String displayName, String streamId) {
		LogStreamDescriptor stream = LogStreamDescriptor.T.create();
		stream.setOrigin(origin(applicationId, nodeId));
		stream.setDisplayName(displayName);
		stream.setStreamId(streamId);
		return stream;
	}

	private static LogOrigin origin(String applicationId, String nodeId) {
		LogOrigin origin = LogOrigin.T.create();
		origin.setApplicationId(applicationId);
		origin.setNodeId(nodeId);
		return origin;
	}
}
