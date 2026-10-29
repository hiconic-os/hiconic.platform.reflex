package hiconic.rx.log.reflection.processing;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.braintribe.model.logging.LogLevel;
import com.braintribe.model.service.api.InstanceId;

import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogFormat;
import hiconic.rx.log.reflection.model.LogStreamDescriptor;
import hiconic.rx.log.reflection.model.api.LogRecordPage;

public class LogFileReaderTest {
	private static final String PROVENTEM_PATTERN =
			"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZ,UTC} %-7level %-75logger '%msg'%ex{full} [%ndc]%n";

	private Path file;
	private InstanceId instanceId;
	private LogStreamDescriptor descriptor;

	@Before
	public void setup() throws Exception {
		file = Files.createTempFile("log-reflection-reader-", ".log");
		instanceId = InstanceId.T.create();
		instanceId.setApplicationId("test-app");
		instanceId.setNodeId("node-1");

		descriptor = LogStreamDescriptor.T.create();
		descriptor.setStreamId("appender:test");
		descriptor.setFormat(LogFormat.PATTERN);
		descriptor.setPattern(PROVENTEM_PATTERN);
	}

	@After
	public void cleanup() throws Exception {
		Files.deleteIfExists(file);
	}

	@Test
	public void parsesProventemPatternAndKeepsMultilineExceptionTogether() throws Exception {
		write("""
				2026-07-23T08:44:25.992+0000 INFO    dt1.proventem.ImportProcessor                                                     'import started' []
				2026-07-23T08:44:26.100+0000 ERROR   dt1.proventem.ImportProcessor                                                     'import failed'java.lang.IllegalStateException: broken
					at dt1.proventem.ImportProcessor.process(ImportProcessor.java:42) [request-7]
				""");

		LogFilter filter = LogFilter.T.create();
		filter.setLevels(Set.of(LogLevel.ERROR));
		filter.setFulltext("broken");

		LogRecordPage page = reader().query(stream(PROVENTEM_PATTERN), filter, null, 20, 0, false);

		assertThat(page.getRecords()).hasSize(1);
		var record = page.getRecords().get(0);
		assertThat(record.getLevel()).isEqualTo(LogLevel.ERROR);
		assertThat(record.getLoggerName()).isEqualTo("dt1.proventem.ImportProcessor");
		assertThat(record.getMessage()).isEqualTo("import failed");
		assertThat(record.getThrowable()).contains("IllegalStateException: broken", "ImportProcessor.java:42");
		assertThat(record.getProperties()).containsEntry("ndc", "request-7");
		assertThat(record.getTimestamp()).isNotNull();
		assertThat(record.getRawText()).contains("\n\tat dt1.proventem");
	}

	@Test
	public void keepsApostrophesInsideQuotedMessages() throws Exception {
		String raw = "2026-10-23T09:07:59.115+0000 INFO    dt1.proventem.processing.init.masterdata.MasterDataInitialization"
				+ "           'Won't register initializer [master-data-initializer:MdSpaceDocumentConfiguration] because no explicit configuration found' []";
		write(raw + "\n");

		LogRecordPage page = reader().query(stream(PROVENTEM_PATTERN), null, null, 20, 0, false);

		assertThat(page.getRecords()).hasSize(1);
		var record = page.getRecords().get(0);
		assertThat(record.getMessage()).isEqualTo(
				"Won't register initializer [master-data-initializer:MdSpaceDocumentConfiguration] because no explicit configuration found");
		assertThat(record.getThrowable()).isNull();
		assertThat(record.getRawText()).isEqualTo(raw);
	}

	@Test
	public void continuesFromByteCursorAfterFileGrowth() throws Exception {
		String simplePattern = "%date %level %logger{10} %msg%n";
		write("2026-07-23 10:44:25,992 INFO dt1.First first\n");

		LogRecordPage first = reader().query(stream(simplePattern), null, null, 20, 0, false);
		assertThat(first.getRecords()).extracting(r -> r.getMessage()).containsExactly("first");

		Files.writeString(file, "2026-07-23 10:44:26,100 WARN dt1.Second second\n", StandardCharsets.UTF_8, APPEND);
		LogRecordPage continued = reader().query(stream(simplePattern), null, first.getNextCursor(), 20, 0, false);

		assertThat(continued.getRecords()).hasSize(1);
		assertThat(continued.getRecords().get(0).getMessage()).isEqualTo("second");
		assertThat(continued.getRecords().get(0).getLevel()).isEqualTo(LogLevel.WARN);
		assertThat(continued.getRecords().get(0).getPosition().getByteOffset()).isPositive();
	}

	@Test
	public void pagesBackwardsWithoutMovingTheTailCursor() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		write("""
				2026-07-23 10:44:21,000 INFO dt1.Test one
				2026-07-23 10:44:22,000 INFO dt1.Test two
				2026-07-23 10:44:23,000 INFO dt1.Test three
				2026-07-23 10:44:24,000 INFO dt1.Test four
				2026-07-23 10:44:25,000 INFO dt1.Test five
				""");

		LogRecordPage latest = reader().query(stream(simplePattern), null, null, null, 2, 0, false);
		assertThat(latest.getRecords()).extracting(r -> r.getMessage()).containsExactly("four", "five");
		assertThat(latest.getMoreBeforeAvailable()).isTrue();
		assertThat(latest.getPreviousCursor()).isNotBlank();
		String tailCursor = latest.getNextCursor();

		LogRecordPage previous = reader().query(stream(simplePattern), null, null, latest.getPreviousCursor(), 2, 0, false);
		assertThat(previous.getRecords()).extracting(r -> r.getMessage()).containsExactly("two", "three");
		assertThat(previous.getNextCursor()).isNull();
		assertThat(previous.getMoreBeforeAvailable()).isTrue();

		Files.writeString(file, "2026-07-23 10:44:26,000 INFO dt1.Test six\n", StandardCharsets.UTF_8, APPEND);
		LogRecordPage continued = reader().query(stream(simplePattern), null, tailCursor, null, 2, 0, false);
		assertThat(continued.getRecords()).extracting(r -> r.getMessage()).containsExactly("six");
	}

	@Test
	public void exposesUnknownFormatAsRawLineRecords() throws Exception {
		write("opaque first line\nopaque second line\n");

		LogRecordPage page = reader().query(
				new LogbackFileStream(file, java.util.List.of(file), descriptor, CompiledLogPattern.raw(instanceId, descriptor.getStreamId())),
				null, null, 20, 0, false);

		assertThat(page.getRecords()).extracting(r -> r.getRawText())
				.containsExactly("opaque first line", "opaque second line");
		assertThat(page.getRecords()).allSatisfy(record -> {
			assertThat(record.getTimestamp()).isNull();
			assertThat(record.getLevel()).isNull();
		});
	}

	@Test
	public void canonicalizesJsonAndRetainsCustomProperties() throws Exception {
		write("""
				{"@timestamp":"2026-07-23T10:44:25.992Z","level":"info","logger_name":"dt1.Import","thread_name":"worker-1","message":"started","tenant":"pv","mdc":{"requestId":"r-7"}}
				{"@timestamp":"2026-07-23T10:44:26.100Z","level":"ERROR","logger_name":"dt1.Import","message":"failed","stack_trace":"java.lang.IllegalStateException: broken","line_number":42}
				""");

		LogRecordPage page = reader().query(
				new LogbackFileStream(file, java.util.List.of(file), descriptor, new JsonLogParser(instanceId, descriptor.getStreamId())),
				null, null, 20, 0, false);

		assertThat(page.getRecords()).hasSize(2);
		var first = page.getRecords().get(0);
		assertThat(first.getTimestamp()).isNotNull();
		assertThat(first.getLevel()).isEqualTo(LogLevel.INFO);
		assertThat(first.getLoggerName()).isEqualTo("dt1.Import");
		assertThat(first.getThreadName()).isEqualTo("worker-1");
		assertThat(first.getMessage()).isEqualTo("started");
		assertThat(first.getProperties()).containsEntry("tenant", "pv").containsEntry("requestId", "r-7");

		var second = page.getRecords().get(1);
		assertThat(second.getLevel()).isEqualTo(LogLevel.ERROR);
		assertThat(second.getThrowable()).contains("IllegalStateException");
		assertThat(second.getSourceLocation().getLineNumber()).isEqualTo(42);
	}

	@Test
	public void filtersParsedFilesByCaseInsensitiveLoggerSubstring() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		write("""
				2026-07-23 10:44:25,992 INFO dt1.proventem.ImportProcessor imported
				2026-07-23 10:44:26,100 INFO dt1.other.ExportProcessor exported
				""");

		LogFilter filter = LogFilter.T.create();
		filter.setLoggerNameContains("PROVENTEM.IMPORT");

		LogRecordPage page = reader().query(stream(simplePattern), filter, null, 20, 0, false);

		assertThat(page.getRecords()).extracting(r -> r.getMessage()).containsExactly("imported");
	}

	@Test
	public void advancesByteCursorAcrossNonMatchingAppends() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		write("2026-07-23 10:44:25,992 INFO dt1.Test initial-match\n");
		LogFilter filter = LogFilter.T.create();
		filter.setFulltext("match");
		LogRecordPage initial = reader().query(stream(simplePattern), filter, null, 20, 0, false);

		Files.writeString(file, "2026-07-23 10:44:26,000 INFO dt1.Test irrelevant\n", StandardCharsets.UTF_8, APPEND);
		LogRecordPage empty = reader().query(stream(simplePattern), filter, initial.getNextCursor(), 20, 0, false);

		assertThat(empty.getRecords()).isEmpty();
		assertThat(empty.getNextCursor()).isNotEqualTo(initial.getNextCursor());

		Files.writeString(file, "2026-07-23 10:44:27,000 INFO dt1.Test later-match\n", StandardCharsets.UTF_8, APPEND);
		LogRecordPage matching = reader().query(stream(simplePattern), filter, empty.getNextCursor(), 20, 0, false);
		assertThat(matching.getRecords()).extracting(r -> r.getMessage()).containsExactly("later-match");
	}

	@Test
	public void pagesLargeSparseAppendsWithoutUnboundedReadsOrLosingTheMatch() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		LogFileReader boundedReader = new LogFileReader(120);
		write("2026-07-23 10:44:20,000 INFO dt1.Test baseline\n");
		LogRecordPage baseline = boundedReader.query(stream(simplePattern), null, null, 20, 0, false);
		Files.writeString(file, """
				2026-07-23 10:44:21,000 INFO dt1.Test irrelevant-one
				2026-07-23 10:44:22,000 INFO dt1.Test irrelevant-two
				2026-07-23 10:44:23,000 INFO dt1.Test irrelevant-three
				2026-07-23 10:44:24,000 INFO dt1.Test sought-match
				""", StandardCharsets.UTF_8, APPEND);
		LogFilter filter = LogFilter.T.create();
		filter.setFulltext("sought-match");

		String cursor = baseline.getNextCursor();
		java.util.List<String> messages = new java.util.ArrayList<>();
		boolean sawBacklog = false;
		for (int pageNumber = 0; pageNumber < 10; pageNumber++) {
			LogRecordPage page = boundedReader.query(stream(simplePattern), filter, cursor, 20, 0, false);
			assertThat(page.getNextCursor()).isNotEqualTo(cursor);
			messages.addAll(page.getRecords().stream().map(record -> record.getMessage()).toList());
			sawBacklog |= page.getMoreAvailable();
			cursor = page.getNextCursor();
			if (!page.getMoreAvailable())
				break;
		}

		assertThat(sawBacklog).isTrue();
		assertThat(messages).containsExactly("sought-match");
	}

	@Test
	public void doesNotConsumeAnIncompletePhysicalRecord() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		String first = "2026-07-23 10:44:25,992 INFO dt1.Test first\n";
		write(first + "2026-07-23 10:44:26,000 INFO dt1.Test sec");

		LogRecordPage initial = reader().query(stream(simplePattern), null, null, 20, 0, false);
		assertThat(initial.getRecords()).extracting(r -> r.getMessage()).containsExactly("first");

		Files.writeString(file, "ond\n", StandardCharsets.UTF_8, APPEND);
		LogRecordPage continued = reader().query(stream(simplePattern), null, initial.getNextCursor(), 20, 0, false);
		assertThat(continued.getRecords()).extracting(r -> r.getMessage()).containsExactly("second");
	}

	@Test
	public void alignsBoundedInitialReadsToTheNextRealRecord() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		write("""
				2026-07-23 10:44:25,992 ERROR dt1.Test failed
				java.lang.IllegalStateException: %s
				\tat dt1.Test.run(Test.java:1)
				2026-07-23 10:44:26,000 INFO dt1.Test visible
				""".formatted("x".repeat(160)));

		LogRecordPage page = new LogFileReader(110).query(stream(simplePattern), null, null, 20, 0, false);

		assertThat(page.getRecords()).extracting(r -> r.getMessage()).containsExactly("visible");
		assertThat(page.getRecords()).noneSatisfy(record -> assertThat(record.getRawText()).startsWith("at dt1.Test"));
	}

	@Test
	public void resetsAnAnchoredCursorAfterCopyTruncateAndFastRegrowth() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		write("2026-07-23 10:44:25,992 INFO dt1.Test old-record\n");
		LogRecordPage old = reader().query(stream(simplePattern), null, null, 20, 0, false);

		Files.writeString(file, """
				2026-07-23 10:44:26,000 INFO dt1.Test replacement-one
				2026-07-23 10:44:27,000 INFO dt1.Test replacement-two
				""", StandardCharsets.UTF_8, TRUNCATE_EXISTING);
		LogRecordPage replaced = reader().query(stream(simplePattern), null, old.getNextCursor(), 20, 0, false);

		assertThat(replaced.getRecords()).extracting(r -> r.getMessage())
				.containsExactly("replacement-one", "replacement-two");
	}

	@Test
	public void resumesAtTheBeginningOfANewActiveFileAfterRotation() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		write("2026-07-23 10:44:25,992 INFO dt1.Test before-rotation\n");
		LogRecordPage beforeRotation = reader().query(stream(simplePattern), null, null, 20, 0, false);
		Files.writeString(file, "2026-07-23 10:44:25,999 INFO dt1.Test unseen-before-rotation\n", StandardCharsets.UTF_8, APPEND);
		Path rotated = Files.createTempFile("log-reflection-reader-rotated-active-", ".log");
		try {
			Files.move(file, rotated, REPLACE_EXISTING);
			write("""
					2026-07-23 10:44:26,000 INFO dt1.Test after-rotation-one
					2026-07-23 10:44:27,000 INFO dt1.Test after-rotation-two
					""");

			LogRecordPage afterRotation = reader().query(stream(simplePattern, java.util.List.of(rotated, file)), null,
					beforeRotation.getNextCursor(), 20, 0, false);

			assertThat(afterRotation.getRecords()).extracting(r -> r.getMessage())
					.containsExactly("unseen-before-rotation", "after-rotation-one", "after-rotation-two");
		} finally {
			Files.deleteIfExists(rotated);
		}
	}

	@Test
	public void resumesAcrossAGzipCompressedRotation() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		write("2026-07-23 10:44:25,992 INFO dt1.Test observed\n");
		LogRecordPage observed = reader().query(stream(simplePattern), null, null, 20, 0, false);
		Files.writeString(file, "2026-07-23 10:44:25,999 INFO dt1.Test unseen-in-gzip\n", StandardCharsets.UTF_8, APPEND);
		Path gzip = Files.createTempFile("log-reflection-reader-rotated-", ".log.gz");
		try {
			try (GZIPOutputStream output = new GZIPOutputStream(Files.newOutputStream(gzip))) {
				Files.copy(file, output);
			}
			write("2026-07-23 10:44:26,000 INFO dt1.Test new-active\n");

			LogRecordPage continued = reader().query(stream(simplePattern, java.util.List.of(gzip, file)), null,
					observed.getNextCursor(), 20, 0, false);

			assertThat(continued.getRecords()).extracting(r -> r.getMessage())
					.containsExactly("unseen-in-gzip", "new-active");
		} finally {
			Files.deleteIfExists(gzip);
		}
	}

	@Test
	public void includesRotatedSegmentsInTheBoundedInitialHistory() throws Exception {
		String simplePattern = "%date %level %logger %msg%n";
		Path rotated = Files.createTempFile("log-reflection-reader-rotated-", ".log");
		try {
			Files.writeString(rotated, "2026-07-22 10:44:25,992 INFO dt1.Old archived\n", StandardCharsets.UTF_8);
			write("2026-07-23 10:44:25,992 INFO dt1.New active\n");
			descriptor.setPattern(simplePattern);
			LogbackFileStream stream = new LogbackFileStream(file, java.util.List.of(rotated, file), descriptor,
					CompiledLogPattern.compile(simplePattern, instanceId, descriptor.getStreamId()));

			LogRecordPage page = reader().query(stream, null, null, 20, 0, true);

			assertThat(page.getRecords()).extracting(r -> r.getMessage()).containsExactly("archived", "active");
		} finally {
			Files.deleteIfExists(rotated);
		}
	}

	private LogFileReader reader() {
		return new LogFileReader();
	}

	private LogbackFileStream stream(String pattern) {
		return stream(pattern, java.util.List.of(file));
	}

	private LogbackFileStream stream(String pattern, java.util.List<Path> paths) {
		descriptor.setPattern(pattern);
		return new LogbackFileStream(file, paths, descriptor,
				CompiledLogPattern.compile(pattern, instanceId, descriptor.getStreamId()));
	}

	private void write(String content) throws Exception {
		Files.writeString(file, content, StandardCharsets.UTF_8);
	}
}
