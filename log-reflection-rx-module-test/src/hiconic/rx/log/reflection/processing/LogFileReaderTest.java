package hiconic.rx.log.reflection.processing;

import static java.nio.file.StandardOpenOption.APPEND;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

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
		descriptor.setPattern(pattern);
		return new LogbackFileStream(file, java.util.List.of(file), descriptor,
				CompiledLogPattern.compile(pattern, instanceId, descriptor.getStreamId()));
	}

	private void write(String content) throws Exception {
		Files.writeString(file, content, StandardCharsets.UTF_8);
	}
}
