package hiconic.rx.log.reflection.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.braintribe.model.service.api.InstanceId;
import com.braintribe.model.logging.LogLevel;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import hiconic.rx.log.reflection.model.LogCapability;
import hiconic.rx.log.reflection.model.LogBundleFormat;
import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogFormat;
import hiconic.rx.log.reflection.model.LogParsingQuality;
import hiconic.rx.log.reflection.model.LogStreamDescriptor;
import hiconic.rx.log.reflection.model.api.CreateLogBundle;

public class LogbackLogStreamInventoryTest {
	private LoggerContext context;
	private Path logFile;
	private Path archivedFile;

	@Before
	public void setup() throws Exception {
		context = new LoggerContext();
		context.start();
		logFile = Files.createTempFile("log-reflection-", ".log");

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%d{ISO8601} %-5level [%thread] %logger - %msg%n");
		encoder.start();

		FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new FileAppender<>();
		appender.setContext(context);
		appender.setName("application");
		appender.setFile(logFile.toString());
		appender.setEncoder(encoder);
		appender.start();
		context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
	}

	@After
	public void cleanup() throws Exception {
		context.stop();
		Files.deleteIfExists(logFile);
		if (archivedFile != null)
			Files.deleteIfExists(archivedFile);
	}

	@Test
	public void reflectsPatternAppenderAndStructuredLiveStream() {
		InstanceId instanceId = InstanceId.T.create();
		instanceId.setApplicationId("test-app");
		instanceId.setNodeId("node-1");

		LogbackLogStreamInventory inventory = new LogbackLogStreamInventory();
		inventory.setLoggerContext(context);
		inventory.setInstanceId(instanceId);

		assertThat(inventory.streams()).hasSize(2);
		LogStreamDescriptor file = inventory.streams().stream()
				.filter(s -> "appender:application".equals(s.getStreamId()))
				.findFirst().orElseThrow();

		assertThat(file.getFormat()).isEqualTo(LogFormat.PATTERN);
		assertThat(file.getParsingQuality()).isEqualTo(LogParsingQuality.PARTIAL);
		assertThat(file.getDisplayName()).isEqualTo("application");
		assertThat(file.getCapabilities()).contains(
				LogCapability.TIMESTAMP, LogCapability.LEVEL, LogCapability.THREAD, LogCapability.LOGGER, LogCapability.FULLTEXT);
		assertThat(file.getSegments()).singleElement().satisfies(segment -> {
			assertThat(segment.getFileName()).isEqualTo(logFile.getFileName().toString());
			assertThat(segment.getActive()).isTrue();
		});
	}

	@Test
	public void reflectsUnstructuredProcessProtocol() throws Exception {
		Path protocol = Files.createTempFile("rx-process-protocol-", ".log");
		try {
			Files.writeString(protocol, "bootstrap output\nwithout canonical fields\n");
			LogbackLogStreamInventory inventory = new LogbackLogStreamInventory();
			inventory.setLoggerContext(context);
			inventory.setInstanceId(instanceId());
			inventory.setProcessProtocolPath(protocol);

			LogStreamDescriptor descriptor = inventory.streams().stream()
					.filter(stream -> LogbackLogStreamInventory.PROCESS_PROTOCOL_STREAM_ID.equals(stream.getStreamId()))
					.findFirst().orElseThrow();

			assertThat(descriptor.getDisplayName()).isEqualTo("Process protocol");
			assertThat(descriptor.getFormat()).isEqualTo(LogFormat.RAW);
			assertThat(descriptor.getParsingQuality()).isEqualTo(LogParsingQuality.RAW_ONLY);
			assertThat(descriptor.getCapabilities()).containsExactly(LogCapability.FULLTEXT);
			assertThat(inventory.fileStream(descriptor.getStreamId()).path()).isEqualTo(protocol.toAbsolutePath().normalize());
		} finally {
			Files.deleteIfExists(protocol);
		}
	}

	@Test
	public void reflectsSegmentsFromTheAppenderRollingPolicy() throws Exception {
		Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
		@SuppressWarnings("unchecked")
		FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> original =
				(FileAppender<ch.qos.logback.classic.spi.ILoggingEvent>) root.getAppender("application");
		root.detachAppender(original);
		original.stop();

		RollingFileAppender<ch.qos.logback.classic.spi.ILoggingEvent> rolling = new RollingFileAppender<>();
		rolling.setContext(context);
		rolling.setName("application");
		rolling.setFile(logFile.toString());
		rolling.setEncoder(original.getEncoder());
		TimeBasedRollingPolicy<ch.qos.logback.classic.spi.ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
		policy.setContext(context);
		policy.setParent(rolling);
		policy.setFileNamePattern(logFile.getParent().resolve("archive-%d{yyyy-MM-dd}.log").toString());
		policy.start();
		rolling.setRollingPolicy(policy);
		rolling.setTriggeringPolicy(policy);
		rolling.start();
		root.addAppender(rolling);

		archivedFile = logFile.getParent().resolve("archive-2026-07-23.log");
		Files.writeString(archivedFile, "archived");

		LogbackLogStreamInventory inventory = new LogbackLogStreamInventory();
		inventory.setLoggerContext(context);
		inventory.setInstanceId(instanceId());
		LogStreamDescriptor file = inventory.streams().stream()
				.filter(stream -> "appender:application".equals(stream.getStreamId()))
				.findFirst().orElseThrow();

		assertThat(file.getSegments()).extracting(segment -> segment.getFileName())
				.containsExactly(archivedFile.getFileName().toString(), logFile.getFileName().toString());
		assertThat(file.getSegments()).extracting(segment -> segment.getActive())
				.containsExactly(false, true);
	}

	@Test
	public void createsAWebRpcReadyTransientZipResource() throws Exception {
		Files.writeString(logFile, "one log line");
		LogbackLogStreamInventory inventory = new LogbackLogStreamInventory();
		inventory.setLoggerContext(context);
		InstanceId instanceId = instanceId();
		inventory.setInstanceId(instanceId);
		CreateLogBundle request = CreateLogBundle.T.create();
		request.setStreamIds(Set.of("appender:application"));

		var resource = new LogBundleWriter().create(inventory, request, instanceId);
		var entries = new ArrayList<String>();
		try (ZipInputStream zip = new ZipInputStream(resource.openStream())) {
			for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				entries.add(entry.getName());
		}

		assertThat(resource.getMimeType()).isEqualTo("application/zip");
		assertThat(resource.getName()).endsWith(".zip");
		assertThat(entries).contains("manifest.json")
				.anySatisfy(name -> assertThat(name).endsWith("/" + logFile.getFileName()));
	}

	@Test
	public void streamsFilteredCanonicalJsonlIntoTheBundle() throws Exception {
		Files.writeString(logFile, """
				2026-07-24 10:00:00,000 INFO  [main] demo.Inventory - ignored
				2026-07-24 10:00:01,000 ERROR [worker] demo.Orders - failed
				caused by a multiline detail
				""");
		LogbackLogStreamInventory inventory = new LogbackLogStreamInventory();
		inventory.setLoggerContext(context);
		InstanceId instanceId = instanceId();
		inventory.setInstanceId(instanceId);

		LogFilter filter = LogFilter.T.create();
		filter.setLevels(Set.of(LogLevel.ERROR));
		CreateLogBundle request = CreateLogBundle.T.create();
		request.setStreamIds(Set.of("appender:application"));
		request.setFormat(LogBundleFormat.CANONICAL_JSONL);
		request.setFilter(filter);

		var resource = new LogBundleWriter().create(inventory, request, instanceId);
		String jsonl = null;
		try (ZipInputStream zip = new ZipInputStream(resource.openStream())) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				if (entry.getName().endsWith(".jsonl"))
					jsonl = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
		}

		assertThat(jsonl).isNotNull();
		assertThat(jsonl.lines()).hasSize(1);
		assertThat(jsonl)
				.contains("\"level\":\"ERROR\"")
				.contains("\"loggerName\":\"demo.Orders\"")
				.contains("caused by a multiline detail")
				.doesNotContain("ignored");
	}

	private static InstanceId instanceId() {
		InstanceId instanceId = InstanceId.T.create();
		instanceId.setApplicationId("test-app");
		instanceId.setNodeId("node-1");
		return instanceId;
	}
}
