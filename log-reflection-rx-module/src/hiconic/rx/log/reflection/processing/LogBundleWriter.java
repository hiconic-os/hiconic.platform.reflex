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
		return create(inventory, request.getStreamIds(), request.getIncludeRotated(), request.getFormat(), request.getFilter(),
				instanceId);
	}

	public Resource create(LogbackLogStreamInventory inventory, CreateLocalLogBundle request, InstanceId instanceId) {
		return create(inventory, request.getStreamIds(), request.getIncludeRotated(), request.getFormat(), request.getFilter(),
				instanceId);
	}

	private Resource create(LogbackLogStreamInventory inventory, Set<String> requestedStreams, boolean includeRotated,
			LogBundleFormat requestedFormat, LogFilter filter, InstanceId instanceId) {
		LogBundleFormat format = requestedFormat == null ? LogBundleFormat.RAW_FILES : requestedFormat;
		if (format == LogBundleFormat.CANONICAL_TEXT)
			throw new IllegalArgumentException("Canonical text bundle serialization is not implemented; use RAW_FILES or CANONICAL_JSONL");

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
			for (BundleEntry entry : entries) {
				String fileName = entry.path.getFileName().toString();
				if (format == LogBundleFormat.CANONICAL_JSONL)
					fileName += ".jsonl";
				String name = unique(names, safe(entry.stream.descriptor().getDisplayName()) + "/" + fileName);
				zip.putNextEntry(new ZipEntry(name));
				if (format == LogBundleFormat.RAW_FILES)
					writeRaw(entry.path, zip);
				else
					writeCanonicalJsonl(entry, filter, zip);
				zip.closeEntry();
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

	private static void writeRaw(Path path, ZipOutputStream zip) throws IOException {
		try (InputStream input = Files.newInputStream(path)) {
			input.transferTo(zip);
		}
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
}
