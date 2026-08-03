package hiconic.rx.log.reflection.processing;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.service.api.InstanceId;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.helper.FileNamePattern;
import hiconic.rx.log.reflection.model.LogCapability;
import hiconic.rx.log.reflection.model.LogFormat;
import hiconic.rx.log.reflection.model.LogParsingQuality;
import hiconic.rx.log.reflection.model.LogSegmentDescriptor;
import hiconic.rx.log.reflection.model.LogStreamDescriptor;
import hiconic.rx.log.reflection.model.LogStreamKind;

/** Reflects the active Logback appenders without changing their configuration. */
public class LogbackLogStreamInventory {
	public static final String PROCESS_PROTOCOL_STREAM_ID = "process:protocol";

	private InstanceId instanceId;
	private LoggerContext loggerContext;
	private Path processProtocolPath;

	@Configurable
	@Required
	public void setInstanceId(InstanceId instanceId) {
		this.instanceId = instanceId;
	}

	@Configurable
	public void setLoggerContext(LoggerContext loggerContext) {
		this.loggerContext = loggerContext;
	}

	@Configurable
	public void setProcessProtocolPath(Path processProtocolPath) {
		this.processProtocolPath = processProtocolPath;
	}

	public List<LogStreamDescriptor> streams() {
		LoggerContext context = loggerContext != null ? loggerContext : (LoggerContext) LoggerFactory.getILoggerFactory();
		List<LogStreamDescriptor> result = new ArrayList<>();
		result.add(structuredLiveDescriptor());
		if (processProtocolPath != null)
			result.add(processProtocolDescriptor());

		Map<Appender<ILoggingEvent>, Boolean> seen = new IdentityHashMap<>();
		for (Logger logger : context.getLoggerList()) {
			for (var it = logger.iteratorForAppenders(); it.hasNext();) {
				Appender<ILoggingEvent> appender = it.next();
				if (appender instanceof FileAppender<?> && seen.put(appender, Boolean.TRUE) == null)
					result.add(descriptor(appender));
			}
		}
		result.sort(Comparator.comparing(LogStreamDescriptor::getKind).thenComparing(LogStreamDescriptor::getDisplayName));
		return result;
	}

	public LogbackFileStream fileStream(String streamId) {
		if (PROCESS_PROTOCOL_STREAM_ID.equals(streamId) && processProtocolPath != null) {
			Path active = processProtocolPath.toAbsolutePath().normalize();
			LogStreamDescriptor descriptor = processProtocolDescriptor();
			return new LogbackFileStream(active, List.of(active), descriptor, CompiledLogPattern.raw(instanceId, streamId));
		}

		LoggerContext context = loggerContext != null ? loggerContext : (LoggerContext) LoggerFactory.getILoggerFactory();
		Map<Appender<ILoggingEvent>, Boolean> seen = new IdentityHashMap<>();
		for (Logger logger : context.getLoggerList()) {
			for (var it = logger.iteratorForAppenders(); it.hasNext();) {
				Appender<ILoggingEvent> appender = it.next();
				if (!(appender instanceof FileAppender<?> fileAppender) || seen.put(appender, Boolean.TRUE) != null)
					continue;
				LogStreamDescriptor descriptor = descriptor(appender);
				if (!descriptor.getStreamId().equals(streamId))
					continue;
				CanonicalLogParser parser = switch (descriptor.getFormat()) {
					case JSON -> new JsonLogParser(instanceId, streamId);
					case PATTERN -> CompiledLogPattern.compile(descriptor.getPattern(), instanceId, streamId);
					default -> CompiledLogPattern.raw(instanceId, streamId);
				};
				Path active = new File(fileAppender.getFile()).toPath().toAbsolutePath().normalize();
				List<Path> paths = descriptor.getSegments().stream()
						.map(LogSegmentDescriptor::getSegmentId)
						.map(Path::of)
						.toList();
				return new LogbackFileStream(active, paths, descriptor, parser);
			}
		}
		throw new IllegalArgumentException("Unknown file log stream: " + streamId);
	}

	private LogStreamDescriptor processProtocolDescriptor() {
		Path active = processProtocolPath.toAbsolutePath().normalize();
		LogStreamDescriptor descriptor = LogStreamDescriptor.T.create();
		descriptor.setStreamId(PROCESS_PROTOCOL_STREAM_ID);
		descriptor.setDisplayName("Process protocol");
		descriptor.setOrigin(LogReflectionModelTools.origin(instanceId));
		descriptor.setKind(LogStreamKind.FILE);
		descriptor.setFormat(LogFormat.RAW);
		descriptor.setParsingQuality(LogParsingQuality.RAW_ONLY);
		descriptor.setCapabilities(EnumSet.of(LogCapability.FULLTEXT));
		descriptor.setSegments(List.of(segment(active.toString())));
		return descriptor;
	}

	private LogStreamDescriptor structuredLiveDescriptor() {
		LogStreamDescriptor descriptor = LogStreamDescriptor.T.create();
		descriptor.setStreamId(StructuredLiveLogCollector.STREAM_ID);
		descriptor.setDisplayName("Live");
		descriptor.setOrigin(LogReflectionModelTools.origin(instanceId));
		descriptor.setKind(LogStreamKind.STRUCTURED_LIVE);
		descriptor.setFormat(LogFormat.STRUCTURED_EVENT);
		descriptor.setParsingQuality(LogParsingQuality.EXACT);
		descriptor.setCapabilities(EnumSet.allOf(LogCapability.class));
		return descriptor;
	}

	private LogStreamDescriptor descriptor(Appender<ILoggingEvent> appender) {
		LogStreamDescriptor descriptor = LogStreamDescriptor.T.create();
		String name = appender.getName() != null ? appender.getName() : appender.getClass().getSimpleName();
		descriptor.setStreamId("appender:" + name);
		descriptor.setOrigin(LogReflectionModelTools.origin(instanceId));
		descriptor.setKind(LogStreamKind.FILE);
		descriptor.getCapabilities().add(LogCapability.FULLTEXT);

		Encoder<ILoggingEvent> encoder = encoder(appender);
		if (encoder instanceof PatternLayoutEncoder patternEncoder) {
			String pattern = patternEncoder.getPattern();
			descriptor.setFormat(LogFormat.PATTERN);
			descriptor.setPattern(pattern);
			descriptor.setParsingQuality(LogParsingQuality.PARTIAL);
			addPatternCapabilities(descriptor, pattern);
		} else if (encoder != null && encoder.getClass().getName().toLowerCase().contains("json")) {
			descriptor.setFormat(LogFormat.JSON);
			descriptor.setParsingQuality(LogParsingQuality.EXACT);
			descriptor.setCapabilities(EnumSet.allOf(LogCapability.class));
		} else {
			descriptor.setFormat(LogFormat.RAW);
			descriptor.setParsingQuality(LogParsingQuality.RAW_ONLY);
		}

		if (appender instanceof FileAppender<?> fileAppender && fileAppender.getFile() != null) {
			descriptor.setDisplayName(name);
			descriptor.getSegments().addAll(segments(fileAppender));
		} else {
			descriptor.setDisplayName(name);
		}
		return descriptor;
	}

	@SuppressWarnings("unchecked")
	private static Encoder<ILoggingEvent> encoder(Appender<ILoggingEvent> appender) {
		if (appender instanceof OutputStreamAppender<?> outputStreamAppender)
			return (Encoder<ILoggingEvent>) outputStreamAppender.getEncoder();
		return null;
	}

	private static LogSegmentDescriptor segment(String path) {
		File file = new File(path);
		LogSegmentDescriptor segment = LogSegmentDescriptor.T.create();
		segment.setSegmentId(file.getAbsolutePath());
		segment.setFileName(file.getName());
		segment.setActive(true);
		if (file.exists()) {
			segment.setSize(file.length());
			segment.setLastModified(new Date(file.lastModified()));
		}
		return segment;
	}

	private static List<LogSegmentDescriptor> segments(FileAppender<?> appender) {
		Path active = Path.of(appender.getFile()).toAbsolutePath().normalize();
		List<Path> paths = new ArrayList<>();
		if (appender instanceof RollingFileAppender<?> rolling
				&& rolling.getRollingPolicy() instanceof RollingPolicyBase policy)
			paths.addAll(rotatedPaths(active, policy));
		paths.sort(Comparator.comparingLong(LogbackLogStreamInventory::lastModified)
				.thenComparing(Path::toString));
		paths.add(active);
		return paths.stream().map(path -> segment(path.toString())).peek(segment ->
				segment.setActive(Path.of(segment.getSegmentId()).equals(active))).toList();
	}

	private static List<Path> rotatedPaths(Path active, RollingPolicyBase policy) {
		String configuredPattern = policy.getFileNamePattern();
		if (configuredPattern == null || configuredPattern.isBlank())
			return List.of();

		Path absolutePattern = Path.of(configuredPattern).toAbsolutePath().normalize();
		Path root = scanRoot(absolutePattern.toString(), active.getParent());
		if (root == null || !Files.isDirectory(root))
			return List.of();

		String regex;
		try {
			regex = new FileNamePattern(absolutePattern.toString(), policy.getContext()).toRegex();
		} catch (RuntimeException e) {
			return List.of();
		}

		int depth = Math.max(1, root.relativize(absolutePattern).getNameCount());
		try (var candidates = Files.walk(root, depth)) {
			return candidates.filter(Files::isRegularFile)
					.map(Path::toAbsolutePath)
					.map(Path::normalize)
					.filter(path -> !path.equals(active))
					.filter(path -> path.toString().matches(regex))
					.limit(10_000)
					.toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	private static Path scanRoot(String pattern, Path fallback) {
		int variable = pattern.indexOf('%');
		if (variable < 0)
			return Path.of(pattern).getParent();
		String prefix = pattern.substring(0, variable);
		int separator = Math.max(prefix.lastIndexOf('/'), prefix.lastIndexOf(File.separatorChar));
		if (separator < 0)
			return fallback;
		String directory = prefix.substring(0, separator + 1);
		return directory.isBlank() ? fallback : Path.of(directory).toAbsolutePath().normalize();
	}

	private static long lastModified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return Long.MIN_VALUE;
		}
	}

	private static void addPatternCapabilities(LogStreamDescriptor descriptor, String pattern) {
		if (pattern == null)
			return;
		if (containsConverter(pattern, "d", "date"))
			descriptor.getCapabilities().add(LogCapability.TIMESTAMP);
		if (containsConverter(pattern, "p", "level", "le"))
			descriptor.getCapabilities().add(LogCapability.LEVEL);
		if (containsConverter(pattern, "c", "logger", "lo"))
			descriptor.getCapabilities().add(LogCapability.LOGGER);
		if (containsConverter(pattern, "t", "thread"))
			descriptor.getCapabilities().add(LogCapability.THREAD);
		if (containsConverter(pattern, "ex", "exception", "throwable"))
			descriptor.getCapabilities().add(LogCapability.THROWABLE);
		if (containsConverter(pattern, "X", "mdc"))
			descriptor.getCapabilities().add(LogCapability.CUSTOM_PROPERTIES);
	}

	private static boolean containsConverter(String pattern, String... words) {
		for (String word : words)
			if (java.util.regex.Pattern.compile("%[-0-9.]*" + java.util.regex.Pattern.quote(word) + "(?:\\{|\\b)")
					.matcher(pattern).find())
				return true;
		return false;
	}
}
