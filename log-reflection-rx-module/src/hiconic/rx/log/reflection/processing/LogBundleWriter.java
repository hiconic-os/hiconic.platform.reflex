package hiconic.rx.log.reflection.processing;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.braintribe.model.resource.Resource;
import com.braintribe.model.service.api.InstanceId;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipes;
import com.fasterxml.jackson.databind.ObjectMapper;

import hiconic.rx.log.reflection.model.LogBundleFormat;
import hiconic.rx.log.reflection.model.LogBundleLayout;
import hiconic.rx.log.reflection.model.LogBundleScope;
import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogRecord;
import hiconic.rx.log.reflection.model.api.CreateLogBundle;
import hiconic.rx.log.reflection.model.api.CreateLocalLogBundle;

/**
 * Creates lossless, file-backed ZIP resources. A file-backed pipe deliberately bounds heap use even for very large
 * logs; Web-RPC subsequently transfers the transient source as multipart data.
 */
public class LogBundleWriter {
	private static final DateTimeFormatter stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final LogFileReader fileReader = new LogFileReader();

	public Resource create(LogbackLogStreamInventory inventory, CreateLogBundle request, InstanceId instanceId) {
		if (modern(request.getScope(), request.getLayouts(), request.getFormats(), request.getFrom(), request.getTo()))
			return createModern(inventory, request.getStreamIds(), request.getScope(), request.getLayouts(), request.getFormats(),
					request.getFrom(), request.getTo(), instanceId);
		return createLegacy(inventory, request.getStreamIds(), request.getIncludeRotated(), request.getFormat(), instanceId);
	}

	public Resource create(LogbackLogStreamInventory inventory, CreateLocalLogBundle request, InstanceId instanceId) {
		if (modern(request.getScope(), request.getLayouts(), request.getFormats(), request.getFrom(), request.getTo()))
			return createModern(inventory, request.getStreamIds(), request.getScope(), request.getLayouts(), request.getFormats(),
					request.getFrom(), request.getTo(), instanceId);
		return createLegacy(inventory, request.getStreamIds(), request.getIncludeRotated(), request.getFormat(), instanceId);
	}

	private static boolean modern(LogBundleScope scope, Set<LogBundleLayout> layouts, Set<LogBundleFormat> formats,
			Date from, Date to) {
		return scope != null || from != null || to != null || layouts != null && !layouts.isEmpty()
				|| formats != null && !formats.isEmpty();
	}

	private Resource createLegacy(LogbackLogStreamInventory inventory, Set<String> requestedStreams, boolean includeRotated,
			LogBundleFormat requestedFormat, InstanceId instanceId) {
		LogBundleFormat format = requestedFormat == null ? LogBundleFormat.RAW_FILES : requestedFormat;

		Set<String> requested = requestedStreams == null ? Set.of() : requestedStreams;
		List<BundleEntry> entries = new ArrayList<>();
		for (var descriptor : inventory.streams()) {
			if (descriptor.getKind() != hiconic.rx.log.reflection.model.LogStreamKind.FILE)
				continue;
			if (!requested.isEmpty() && !requested.contains(descriptor.getStreamId()))
				continue;
			LogbackFileStream stream = inventory.fileStream(descriptor.getStreamId());
			for (Path path : stream.segmentPaths()) {
				boolean active = path.equals(stream.path());
				if (!active && !includeRotated)
					continue;
				if (Files.isRegularFile(path))
					entries.add(new BundleEntry(stream, path, active));
			}
		}
		if (entries.isEmpty())
			throw new IllegalArgumentException("The selection contains no readable file log segments");

		String baseName = "logs-" + safe(instanceId.getApplicationId()) + "-" + safe(instanceId.getNodeId()) + "-"
				+ stamp.format(Instant.now());
		StreamPipe pipe = StreamPipes.fileBackedFactory().newPipe(baseName);
		try (OutputStream output = pipe.openOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
			writeManifest(zip, instanceId, entries, includeRotated, format);
			Set<String> names = new HashSet<>();
			if (format == LogBundleFormat.RAW_FILES) {
				for (BundleEntry entry : entries) {
					String fileName = entry.path.getFileName().toString();
					String name = unique(names, safe(entry.stream.descriptor().getDisplayName()) + "/" + fileName);
					zip.putNextEntry(new ZipEntry(name));
					writeRaw(entry.path, zip);
					zip.closeEntry();
				}
			} else {
				for (List<BundleEntry> streamEntries : entriesByStream(entries).values()) {
					BundleEntry first = streamEntries.get(0);
					String extension = format == LogBundleFormat.CANONICAL_JSONL ? ".jsonl" : ".log";
					String displayName = safe(first.stream.descriptor().getDisplayName());
					String name = unique(names, displayName + "/merged" + extension);
					zip.putNextEntry(new ZipEntry(name));
					for (BundleEntry entry : streamEntries) {
						if (format == LogBundleFormat.CANONICAL_JSONL)
							writeCanonicalJsonl(entry, zip);
						else
							writeCanonicalText(entry, zip);
					}
					zip.closeEntry();
				}
			}
			zip.finish();
		} catch (IOException | UncheckedIOException e) {
			pipe.close();
			throw new IllegalStateException("Could not create log bundle", e);
		}

		Resource resource = Resource.createTransient(() -> new FilterInputStream(pipe.openInputStream()) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					pipe.close();
				}
			}
		});
		resource.setName(baseName + ".zip");
		resource.setMimeType("application/zip");
		return resource;
	}

	private Resource createModern(LogbackLogStreamInventory inventory, Set<String> requestedStreams, LogBundleScope requestedScope,
			Set<LogBundleLayout> requestedLayouts, Set<LogBundleFormat> requestedFormats, Date from, Date to,
			InstanceId instanceId) {
		LogBundleScope scope = requestedScope == null ? LogBundleScope.CURRENT : requestedScope;
		EnumSet<LogBundleLayout> layouts = requestedLayouts == null || requestedLayouts.isEmpty()
				? EnumSet.of(LogBundleLayout.SEPARATE, LogBundleLayout.MERGED)
				: EnumSet.copyOf(requestedLayouts);
		EnumSet<LogBundleFormat> formats = requestedFormats == null || requestedFormats.isEmpty()
				? EnumSet.of(LogBundleFormat.NATURAL)
				: EnumSet.copyOf(requestedFormats);
		if (formats.remove(LogBundleFormat.RAW_FILES))
			formats.add(LogBundleFormat.NATURAL);
		if (scope == LogBundleScope.CUSTOM_RANGE) {
			if (from == null || to == null)
				throw new IllegalArgumentException("A custom log bundle range requires both from and to");
			if (from.after(to))
				throw new IllegalArgumentException("The custom log bundle range starts after it ends");
		} else if (from != null || to != null) {
			throw new IllegalArgumentException("Log bundle boundaries require the CUSTOM_RANGE scope");
		}

		Set<String> requested = requestedStreams == null ? Set.of() : requestedStreams;
		List<BundleEntry> entries = collectEntries(inventory, requested, scope != LogBundleScope.CURRENT);
		if (entries.isEmpty())
			throw new IllegalArgumentException("The selection contains no readable file log segments");

		LogFilter range = scope == LogBundleScope.CUSTOM_RANGE ? range(from, to) : null;
		Map<Path, SegmentStats> stats = new LinkedHashMap<>();
		if (range != null)
			for (BundleEntry entry : entries)
				stats.put(entry.path, inspect(entry, range));
		List<BundleEntry> contributing = range == null ? entries : entries.stream()
				.filter(entry -> stats.get(entry.path).selectedRecords > 0)
				.toList();

		String baseName = "logs-" + safe(instanceId.getApplicationId()) + "-" + safe(instanceId.getNodeId()) + "-"
				+ stamp.format(Instant.now());
		StreamPipe pipe = StreamPipes.fileBackedFactory().newPipe(baseName);
		try (OutputStream output = pipe.openOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
			writeModernManifest(zip, instanceId, scope, layouts, formats, from, to, entries, stats);
			writeModernEntries(zip, contributing, layouts, formats, range);
			zip.finish();
		} catch (IOException | UncheckedIOException e) {
			pipe.close();
			throw new IllegalStateException("Could not create log bundle", e);
		}
		return resource(pipe, baseName);
	}

	private static List<BundleEntry> collectEntries(LogbackLogStreamInventory inventory, Set<String> requestedStreams,
			boolean includeRotated) {
		List<BundleEntry> entries = new ArrayList<>();
		for (var descriptor : inventory.streams()) {
			if (descriptor.getKind() != hiconic.rx.log.reflection.model.LogStreamKind.FILE)
				continue;
			if (!requestedStreams.isEmpty() && !requestedStreams.contains(descriptor.getStreamId()))
				continue;
			LogbackFileStream stream = inventory.fileStream(descriptor.getStreamId());
			for (Path path : stream.segmentPaths()) {
				boolean active = path.equals(stream.path());
				if ((active || includeRotated) && Files.isRegularFile(path))
					entries.add(new BundleEntry(stream, path, active));
			}
		}
		return entries;
	}

	private void writeModernEntries(ZipOutputStream zip, List<BundleEntry> entries, Set<LogBundleLayout> layouts,
			Set<LogBundleFormat> formats, LogFilter range) throws IOException {
		Set<String> names = new HashSet<>();
		for (List<BundleEntry> streamEntries : entriesByStream(entries).values()) {
			String stream = safe(streamEntries.get(0).stream.descriptor().getStreamId());
			if (layouts.contains(LogBundleLayout.SEPARATE)) {
				for (BundleEntry entry : streamEntries) {
					String segment = safe(entry.path.getFileName().toString());
					if (formats.contains(LogBundleFormat.NATURAL)) {
						String name = unique(names, stream + "/separate/natural/" + segment + (range == null ? "" : ".log"));
						zip.putNextEntry(new ZipEntry(name));
						if (range == null)
							writeRaw(entry.path, zip);
						else
							writeNaturalText(entry, range, zip);
						zip.closeEntry();
					}
					if (formats.contains(LogBundleFormat.CANONICAL_TEXT)) {
						zip.putNextEntry(new ZipEntry(unique(names, stream + "/separate/canonical-text/" + segment + ".log")));
						writeCanonicalText(entry, range, zip);
						zip.closeEntry();
					}
					if (formats.contains(LogBundleFormat.CANONICAL_JSONL)) {
						zip.putNextEntry(new ZipEntry(unique(names, stream + "/separate/jsonl/" + segment + ".jsonl")));
						writeCanonicalJsonl(entry, range, zip);
						zip.closeEntry();
					}
				}
			}
			if (layouts.contains(LogBundleLayout.MERGED)) {
				if (formats.contains(LogBundleFormat.NATURAL) || formats.contains(LogBundleFormat.CANONICAL_TEXT)) {
					zip.putNextEntry(new ZipEntry(unique(names, stream + "/merged/canonical.log")));
					for (BundleEntry entry : streamEntries)
						writeCanonicalText(entry, range, zip);
					zip.closeEntry();
				}
				if (formats.contains(LogBundleFormat.CANONICAL_JSONL)) {
					zip.putNextEntry(new ZipEntry(unique(names, stream + "/merged/canonical.jsonl")));
					for (BundleEntry entry : streamEntries)
						writeCanonicalJsonl(entry, range, zip);
					zip.closeEntry();
				}
			}
		}
	}

	private SegmentStats inspect(BundleEntry entry, LogFilter range) {
		long[] counts = new long[3];
		fileReader.forEachRecord(entry.stream, entry.path, null, record -> {
			counts[0]++;
			if (record.getTimestamp() == null)
				counts[2]++;
			if (LogRecordFiltering.matches(record, range))
				counts[1]++;
		});
		return new SegmentStats(counts[0], counts[1], counts[2]);
	}

	private static LogFilter range(Date from, Date to) {
		LogFilter filter = LogFilter.T.create();
		filter.setFrom(from);
		filter.setTo(to);
		return filter;
	}

	private void writeNaturalText(BundleEntry entry, LogFilter filter, ZipOutputStream zip) {
		fileReader.forEachRecord(entry.stream, entry.path, filter, record -> write(zip, terminated(record.getRawText())));
	}

	private static void write(ZipOutputStream zip, String text) {
		try {
			zip.write(text.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static Resource resource(StreamPipe pipe, String baseName) {
		Resource resource = Resource.createTransient(() -> new FilterInputStream(pipe.openInputStream()) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					pipe.close();
				}
			}
		});
		resource.setName(baseName + ".zip");
		resource.setMimeType("application/zip");
		return resource;
	}

	private static void writeRaw(Path path, ZipOutputStream zip) throws IOException {
		try (InputStream input = Files.newInputStream(path)) {
			input.transferTo(zip);
		}
	}

	private void writeCanonicalJsonl(BundleEntry entry, ZipOutputStream zip) {
		writeCanonicalJsonl(entry, null, zip);
	}

	private void writeCanonicalJsonl(BundleEntry entry, LogFilter filter, ZipOutputStream zip) {
		fileReader.forEachRecord(entry.stream, entry.path, filter, record -> {
			try {
				zip.write(objectMapper.writeValueAsBytes(canonicalRecord(record)));
				zip.write('\n');
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	private void writeCanonicalText(BundleEntry entry, ZipOutputStream zip) {
		writeCanonicalText(entry, null, zip);
	}

	private void writeCanonicalText(BundleEntry entry, LogFilter filter, ZipOutputStream zip) {
		fileReader.forEachRecord(entry.stream, entry.path, filter, record -> {
			try {
				zip.write(canonicalText(record).getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	private static String canonicalText(LogRecord record) {
		if (record.getTimestamp() == null && record.getLevel() == null && record.getLoggerName() == null)
			return terminated(record.getRawText());

		String timestamp = record.getTimestamp() == null ? "-" : record.getTimestamp().toInstant().toString();
		String level = record.getLevel() == null ? "-" : record.getLevel().name();
		String application = record.getOrigin() == null ? null : record.getOrigin().getApplicationId();
		String node = record.getOrigin() == null ? null : record.getOrigin().getNodeId();
		String origin = safe(application) + "@" + safe(node);
		String logger = record.getLoggerName() == null ? "-" : record.getLoggerName();
		String message = record.getMessage() == null ? "" : record.getMessage();
		StringBuilder result = new StringBuilder()
				.append(timestamp).append(' ')
				.append(level).append(" [").append(origin).append("] ")
				.append(logger).append(" - ").append(message).append('\n');
		if (record.getThrowable() != null && !record.getThrowable().isBlank())
			result.append(terminated(record.getThrowable()));
		return result.toString();
	}

	private static String terminated(String value) {
		if (value == null || value.isEmpty())
			return "\n";
		return value.endsWith("\n") ? value : value + '\n';
	}

	private static Map<String, List<BundleEntry>> entriesByStream(List<BundleEntry> entries) {
		Map<String, List<BundleEntry>> result = new LinkedHashMap<>();
		for (BundleEntry entry : entries)
			result.computeIfAbsent(entry.stream.descriptor().getStreamId(), ignored -> new ArrayList<>()).add(entry);
		return result;
	}

	private static Map<String, Object> canonicalRecord(LogRecord record) {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("timestamp", record.getTimestamp() == null ? null : record.getTimestamp().toInstant().toString());
		json.put("level", record.getLevel() == null ? null : record.getLevel().name());

		if (record.getOrigin() != null) {
			Map<String, Object> origin = new LinkedHashMap<>();
			origin.put("applicationId", record.getOrigin().getApplicationId());
			origin.put("nodeId", record.getOrigin().getNodeId());
			json.put("origin", origin);
		} else {
			json.put("origin", null);
		}

		json.put("streamId", record.getStreamId());
		json.put("loggerName", record.getLoggerName());
		json.put("threadName", record.getThreadName());
		json.put("message", record.getMessage());
		json.put("throwable", record.getThrowable());

		if (record.getSourceLocation() != null) {
			Map<String, Object> source = new LinkedHashMap<>();
			source.put("className", record.getSourceLocation().getClassName());
			source.put("methodName", record.getSourceLocation().getMethodName());
			source.put("fileName", record.getSourceLocation().getFileName());
			source.put("lineNumber", record.getSourceLocation().getLineNumber());
			json.put("sourceLocation", source);
		} else {
			json.put("sourceLocation", null);
		}

		json.put("properties", record.getProperties() == null ? Map.of() : new LinkedHashMap<>(record.getProperties()));
		json.put("rawText", record.getRawText());

		if (record.getPosition() != null) {
			Map<String, Object> position = new LinkedHashMap<>();
			position.put("sequence", record.getPosition().getSequence());
			position.put("byteOffset", record.getPosition().getByteOffset());
			json.put("position", position);
		} else {
			json.put("position", null);
		}
		return json;
	}

	/**
	 * Combines complete node contributions without materializing them in heap. Node ZIP entries retain their selected
	 * raw or canonical representation; only their container is rewritten to give every node a stable namespace.
	 */
	public Resource combine(Map<String, Resource> contributions, Map<String, String> errors, String applicationId) {
		if (contributions == null || contributions.isEmpty())
			throw new IllegalArgumentException("No node returned a readable log bundle contribution");

		String baseName = "logs-" + safe(applicationId) + "-all-nodes-" + stamp.format(Instant.now());
		StreamPipe pipe = StreamPipes.fileBackedFactory().newPipe(baseName);
		try (OutputStream output = pipe.openOutputStream(); ZipOutputStream zip = new ZipOutputStream(output)) {
			writeClusterManifest(zip, contributions.keySet(), errors);
			Set<String> names = new HashSet<>();
			names.add("manifest.json");
			for (Map.Entry<String, Resource> contribution : contributions.entrySet())
				copyContribution(zip, names, contribution.getKey(), contribution.getValue());
			zip.finish();
		} catch (IOException e) {
			pipe.close();
			throw new IllegalStateException("Could not combine node log bundles", e);
		}

		Resource resource = Resource.createTransient(() -> new FilterInputStream(pipe.openInputStream()) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					pipe.close();
				}
			}
		});
		resource.setName(baseName + ".zip");
		resource.setMimeType("application/zip");
		return resource;
	}

	private static void copyContribution(ZipOutputStream target, Set<String> names, String origin, Resource contribution)
			throws IOException {
		String prefix = safe(origin) + "/";
		try (InputStream input = contribution.openStream(); ZipInputStream source = new ZipInputStream(input)) {
			ZipEntry sourceEntry;
			while ((sourceEntry = source.getNextEntry()) != null) {
				String relativeName = safeEntryName(sourceEntry.getName());
				if (relativeName.isEmpty())
					continue;
				String name = unique(names, prefix + relativeName);
				ZipEntry targetEntry = new ZipEntry(name);
				if (sourceEntry.getTime() >= 0)
					targetEntry.setTime(sourceEntry.getTime());
				target.putNextEntry(targetEntry);
				if (!sourceEntry.isDirectory())
					source.transferTo(target);
				target.closeEntry();
				source.closeEntry();
			}
		}
	}

	private static String safeEntryName(String value) {
		if (value == null)
			return "";
		String normalized = value.replace('\\', '/');
		while (normalized.startsWith("/"))
			normalized = normalized.substring(1);
		List<String> parts = new ArrayList<>();
		for (String part : normalized.split("/")) {
			if (part.isBlank() || ".".equals(part))
				continue;
			if ("..".equals(part))
				throw new IllegalArgumentException("Unsafe ZIP entry in node log bundle: " + value);
			parts.add(safe(part));
		}
		return String.join("/", parts);
	}

	private static void writeClusterManifest(ZipOutputStream zip, Set<String> origins, Map<String, String> errors)
			throws IOException {
		Map<String, String> stableErrors = errors == null ? Map.of() : new LinkedHashMap<>(errors);
		StringBuilder json = new StringBuilder();
		json.append("{\n  \"createdAt\": \"").append(Instant.now()).append("\",\n  \"nodes\": [");
		boolean first = true;
		for (String origin : origins) {
			if (!first)
				json.append(", ");
			json.append('"').append(json(origin)).append('"');
			first = false;
		}
		json.append("],\n  \"errors\": {");
		first = true;
		for (Map.Entry<String, String> error : stableErrors.entrySet()) {
			if (!first)
				json.append(',');
			json.append("\n    \"").append(json(error.getKey())).append("\": \"")
					.append(json(error.getValue())).append('"');
			first = false;
		}
		if (!stableErrors.isEmpty())
			json.append('\n').append("  ");
		json.append("}\n}\n");
		zip.putNextEntry(new ZipEntry("manifest.json"));
		zip.write(json.toString().getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private static void writeManifest(ZipOutputStream zip, InstanceId instanceId, List<BundleEntry> entries,
			boolean includeRotated, LogBundleFormat format) throws IOException {
		String json = """
				{
				  "applicationId": "%s",
				  "nodeId": "%s",
				  "createdAt": "%s",
				  "format": "%s",
				  "includeRotated": %s,
				  "fileCount": %d
				}
				""".formatted(json(instanceId.getApplicationId()), json(instanceId.getNodeId()), Instant.now(),
						format.name(), includeRotated, entries.size());
		zip.putNextEntry(new ZipEntry("manifest.json"));
		zip.write(json.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private static void writeModernManifest(ZipOutputStream zip, InstanceId instanceId, LogBundleScope scope,
			Set<LogBundleLayout> layouts, Set<LogBundleFormat> formats, Date from, Date to, List<BundleEntry> entries,
			Map<Path, SegmentStats> stats) throws IOException {
		StringBuilder value = new StringBuilder();
		value.append("{\n")
				.append("  \"applicationId\": \"").append(json(instanceId.getApplicationId())).append("\",\n")
				.append("  \"nodeId\": \"").append(json(instanceId.getNodeId())).append("\",\n")
				.append("  \"createdAt\": \"").append(Instant.now()).append("\",\n")
				.append("  \"scope\": \"").append(scope.name()).append("\",\n")
				.append("  \"from\": ").append(date(from)).append(",\n")
				.append("  \"to\": ").append(date(to)).append(",\n")
				.append("  \"layouts\": ").append(enumJson(layouts)).append(",\n")
				.append("  \"formats\": ").append(enumJson(formats)).append(",\n")
				.append("  \"segments\": [");
		boolean first = true;
		for (BundleEntry entry : entries) {
			if (!first)
				value.append(',');
			SegmentStats segment = stats.get(entry.path);
			value.append("\n    {\"streamId\": \"").append(json(entry.stream.descriptor().getStreamId()))
					.append("\", \"fileName\": \"").append(json(entry.path.getFileName().toString()))
					.append("\", \"active\": ").append(entry.active);
			if (segment != null)
				value.append(", \"recordCount\": ").append(segment.totalRecords)
						.append(", \"selectedRecordCount\": ").append(segment.selectedRecords)
						.append(", \"recordsWithoutTimestamp\": ").append(segment.recordsWithoutTimestamp);
			value.append('}');
			first = false;
		}
		if (!entries.isEmpty())
			value.append('\n').append("  ");
		value.append("]\n}\n");
		zip.putNextEntry(new ZipEntry("manifest.json"));
		zip.write(value.toString().getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}

	private static String date(Date value) {
		return value == null ? "null" : "\"" + value.toInstant() + "\"";
	}

	private static String enumJson(Set<? extends Enum<?>> values) {
		return values.stream().map(value -> "\"" + value.name() + "\"").collect(java.util.stream.Collectors.joining(", ", "[", "]"));
	}

	private static String unique(Set<String> names, String proposed) {
		if (names.add(proposed))
			return proposed;
		int dot = proposed.lastIndexOf('.');
		String base = dot < 0 ? proposed : proposed.substring(0, dot);
		String suffix = dot < 0 ? "" : proposed.substring(dot);
		for (int i = 2;; i++) {
			String candidate = base + "-" + i + suffix;
			if (names.add(candidate))
				return candidate;
		}
	}

	private static String safe(String value) {
		if (value == null || value.isBlank())
			return "unknown";
		return value.replaceAll("[^A-Za-z0-9._-]+", "_");
	}

	private static String json(String value) {
		return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private record BundleEntry(LogbackFileStream stream, Path path, boolean active) {
	}

	private record SegmentStats(long totalRecords, long selectedRecords, long recordsWithoutTimestamp) {
	}
}
