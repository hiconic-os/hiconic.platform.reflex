package hiconic.rx.log.reflection.processing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogRecord;
import hiconic.rx.log.reflection.model.api.LogRecordPage;

/** Reads active UTF-8 log files and turns physical records into canonical records. */
public class LogFileReader {
	private static final long MAX_READ_BYTES = 64L * 1024 * 1024;
	private static final int CURSOR_ANCHOR_BYTES = 64;

	private final long maxReadBytes;

	public LogFileReader() {
		this(MAX_READ_BYTES);
	}

	LogFileReader(long maxReadBytes) {
		this.maxReadBytes = Math.max(1, maxReadBytes);
	}

	public LogRecordPage query(LogbackFileStream stream, LogFilter filter, String cursor, int requestedLimit, long waitMillis,
			boolean includeRotated) {
		return query(stream, filter, cursor, null, requestedLimit, waitMillis, includeRotated);
	}

	public LogRecordPage query(LogbackFileStream stream, LogFilter filter, String cursor, String beforeCursor, int requestedLimit,
			long waitMillis, boolean includeRotated) {
		if (beforeCursor != null && !beforeCursor.isBlank())
			return queryBefore(stream, filter, beforeCursor, requestedLimit);
		if (includeRotated && (cursor == null || cursor.isBlank()) && stream.segmentPaths().size() > 1)
			return queryHistory(stream, filter, requestedLimit);
		try {
			Path path = stream.path();
			FileState state = state(path);
			FileCursor parsedCursor = FileCursor.parse(cursor);
			if (parsedCursor != null && !valid(path, state, parsedCursor)) {
				int segmentIndex = findCursorSegment(stream.segmentPaths(), path, parsedCursor);
				if (segmentIndex >= 0)
					return queryAcrossRotation(stream, filter, parsedCursor, segmentIndex, requestedLimit);
			}
			long offset = valid(path, state, parsedCursor) ? parsedCursor.offset : 0;

			if (cursor != null && offset >= state.size && waitMillis > 0)
				state = awaitGrowth(path, state, Math.min(waitMillis, 30_000));
			if (!valid(path, state, parsedCursor))
				offset = 0;

			long readStart = offset;
			if (cursor == null && state.size - readStart > maxReadBytes)
				readStart = state.size - maxReadBytes;

			long readEnd = Math.min(state.size, readStart + maxReadBytes);
			boolean unreadBytes = readEnd < state.size;
			ReadSlice slice = read(path, readStart, readEnd);
			slice = completePhysicalLines(slice);
			if (unreadBytes && slice.bytes.length == 0)
				throw new IOException("A single physical log record exceeds the " + maxReadBytes + " byte query window");
			if (readStart > 0 && cursor == null)
				slice = alignFirstRecord(path, slice, stream.parser());

			List<PhysicalRecord> physicalRecords = records(slice.bytes, slice.absoluteStart, stream.parser());
			List<PhysicalRecord> matches = physicalRecords.stream()
					.filter(record -> LogRecordFiltering.matches(record.record, filter))
					.toList();

			int limit = Math.max(1, Math.min(requestedLimit, 10_000));
			boolean initial = cursor == null;
			boolean moreAvailable = !initial && (matches.size() > limit || unreadBytes);
			boolean moreBeforeAvailable = initial && (matches.size() > limit || readStart > 0);
			List<PhysicalRecord> selected;
			if (initial && matches.size() > limit)
				selected = matches.subList(matches.size() - limit, matches.size());
			else
				selected = matches.subList(0, Math.min(limit, matches.size()));

			LogRecordPage response = LogRecordPage.T.create();
			List<LogRecord> page = selected.stream().map(record -> record.record).toList();
			response.setRecords(page);
			response.setObservedProperties(LogRecordFiltering.observedProperties(page));
			response.setMoreAvailable(moreAvailable);
			response.setMoreBeforeAvailable(moreBeforeAvailable);
			if (moreBeforeAvailable) {
				long previousOffset = selected.isEmpty() ? slice.absoluteStart : startOffset(selected.get(0));
				response.setPreviousCursor(cursor(path, state, previousOffset).externalForm());
			}

			long nextOffset;
			if (matches.size() > limit)
				nextOffset = selected.get(selected.size() - 1).endOffset;
			else
				nextOffset = slice.absoluteStart + slice.bytes.length;
			response.setNextCursor(cursor(path, state, nextOffset).externalForm());
			return response;
		} catch (IOException e) {
			throw new IllegalStateException("Could not read log stream " + stream.descriptor().getStreamId(), e);
		}
	}

	/**
	 * Continues a cursor which used to point into the active file after that file became a rotated segment. This closes the
	 * otherwise unavoidable gap between the last poll and Logback's rotation. Rotated segments are consulted only to resume
	 * an existing cursor; they are not exposed as history unless the request explicitly asks for history.
	 */
	private LogRecordPage queryAcrossRotation(LogbackFileStream stream, LogFilter filter, FileCursor cursor, int segmentIndex,
			int requestedLimit) throws IOException {
		List<SegmentRecord> matches = new ArrayList<>();
		List<Path> paths = stream.segmentPaths();
		long remaining = maxReadBytes;
		SegmentProgress progress = null;
		boolean unread = false;

		for (int i = segmentIndex; i < paths.size() && remaining > 0; i++) {
			Path path = paths.get(i);
			long start = i == segmentIndex ? cursor.offset : 0;
			SegmentSlice segment = readSegment(path, start, remaining, path.equals(stream.path()));
			ReadSlice slice = segment.slice;
			remaining -= slice.bytes.length;
			for (PhysicalRecord record : records(slice.bytes, slice.absoluteStart, stream.parser()))
				if (LogRecordFiltering.matches(record.record, filter))
					matches.add(new SegmentRecord(path, segment.state, record));
			progress = new SegmentProgress(path, segment.state, slice.absoluteStart + slice.bytes.length);
			unread = segment.more || i + 1 < paths.size();
			if (segment.more)
				break;
		}

		int limit = Math.max(1, Math.min(requestedLimit, 10_000));
		boolean moreAvailable = matches.size() > limit || unread;
		List<SegmentRecord> selected = matches.subList(0, Math.min(limit, matches.size()));
		LogRecordPage response = LogRecordPage.T.create();
		List<LogRecord> page = selected.stream().map(record -> record.record.record).toList();
		response.setRecords(page);
		response.setObservedProperties(LogRecordFiltering.observedProperties(page));
		response.setMoreAvailable(moreAvailable);
		response.setMoreBeforeAvailable(false);

		if (matches.size() > limit) {
			SegmentRecord last = selected.get(selected.size() - 1);
			response.setNextCursor(cursor(last.path, last.state, last.record.endOffset).externalForm());
		} else if (progress != null) {
			response.setNextCursor(cursor(progress.path, progress.state, progress.offset).externalForm());
		} else {
			FileState active = state(stream.path());
			response.setNextCursor(cursor(stream.path(), active, 0).externalForm());
		}
		return response;
	}

	private static int findCursorSegment(List<Path> paths, Path active, FileCursor cursor) throws IOException {
		for (int i = paths.size() - 1; i >= 0; i--) {
			Path candidate = paths.get(i);
			if (!candidate.equals(active) && Files.isRegularFile(candidate) && matchesCursor(candidate, cursor))
				return i;
		}
		return -1;
	}

	private static boolean matchesCursor(Path path, FileCursor cursor) throws IOException {
		if (cursor.offset <= 0)
			return false;
		if (!compressed(path.getFileName().toString().toLowerCase(Locale.ROOT))) {
			FileState state = state(path);
			return cursor.offset <= state.size && cursor.anchor != null && cursor.anchor.equals(anchor(path, cursor.offset));
		}
		return cursor.anchor != null && cursor.anchor.equals(logicalAnchor(path, cursor.offset));
	}

	private static SegmentSlice readSegment(Path path, long start, long maximumBytes, boolean active) throws IOException {
		FileState state = state(path);
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		ReadSlice slice;
		boolean more;
		if (compressed(name)) {
			LogicalRead logical = readLogical(path, start, maximumBytes);
			slice = new ReadSlice(start, logical.bytes);
			more = !logical.eof;
		} else {
			long end = Math.min(state.size, start + maximumBytes);
			slice = read(path, start, end);
			more = end < state.size;
		}
		if (active || more) {
			ReadSlice complete = completePhysicalLines(slice);
			slice = complete;
		}
		if (more && slice.bytes.length == 0)
			throw new IOException("A single physical log record exceeds the query window while resuming across rotation");
		return new SegmentSlice(state, slice, more);
	}

	private static LogicalRead readLogical(Path path, long start, long maximumBytes) throws IOException {
		int capacity = Math.toIntExact(Math.min(Integer.MAX_VALUE, maximumBytes));
		ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(capacity, 64 * 1024));
		try (InputStream input = openContent(path)) {
			if (!skip(input, start))
				return new LogicalRead(new byte[0], true);
			byte[] buffer = new byte[64 * 1024];
			int remaining = capacity;
			while (remaining > 0) {
				int read = input.read(buffer, 0, Math.min(buffer.length, remaining));
				if (read < 0)
					return new LogicalRead(output.toByteArray(), true);
				if (read > 0) {
					output.write(buffer, 0, read);
					remaining -= read;
				}
			}
			return new LogicalRead(output.toByteArray(), input.read() < 0);
		}
	}

	private static boolean skip(InputStream input, long count) throws IOException {
		long remaining = count;
		byte[] buffer = new byte[64 * 1024];
		while (remaining > 0) {
			long skipped = input.skip(remaining);
			if (skipped > 0) {
				remaining -= skipped;
				continue;
			}
			int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
			if (read < 0)
				return false;
			remaining -= read;
		}
		return true;
	}

	private LogRecordPage queryBefore(LogbackFileStream stream, LogFilter filter, String beforeCursor, int requestedLimit) {
		try {
			Path path = stream.path();
			FileState state = state(path);
			FileCursor cursor = FileCursor.parse(beforeCursor);
			long end = valid(path, state, cursor) ? cursor.offset : state.size;
			long start = Math.max(0, end - maxReadBytes);
			ReadSlice slice = read(path, start, end);
			if (start > 0)
				slice = alignFirstRecord(path, slice, stream.parser());

			List<PhysicalRecord> matches = records(slice.bytes, slice.absoluteStart, stream.parser()).stream()
					.filter(record -> LogRecordFiltering.matches(record.record, filter))
					.toList();
			int limit = Math.max(1, Math.min(requestedLimit, 10_000));
			List<PhysicalRecord> selected = matches.size() > limit
					? matches.subList(matches.size() - limit, matches.size())
					: matches;
			boolean moreBeforeAvailable = matches.size() > limit || start > 0;

			LogRecordPage response = LogRecordPage.T.create();
			List<LogRecord> page = selected.stream().map(record -> record.record).toList();
			response.setRecords(page);
			response.setObservedProperties(LogRecordFiltering.observedProperties(page));
			response.setMoreAvailable(false);
			response.setMoreBeforeAvailable(moreBeforeAvailable);
			if (moreBeforeAvailable) {
				long previousOffset = selected.isEmpty() ? slice.absoluteStart : startOffset(selected.get(0));
				response.setPreviousCursor(cursor(path, state, previousOffset).externalForm());
			}
			return response;
		} catch (IOException e) {
			throw new IllegalStateException("Could not read earlier log records from " + stream.descriptor().getStreamId(), e);
		}
	}

	private LogRecordPage queryHistory(LogbackFileStream stream, LogFilter filter, int requestedLimit) {
		try {
			List<List<PhysicalRecord>> newestFirst = new ArrayList<>();
			long remaining = maxReadBytes;
			List<Path> paths = stream.segmentPaths();
			for (int i = paths.size() - 1; i >= 0 && remaining > 0; i--) {
				Path path = paths.get(i);
				ReadSlice slice = readTail(path, remaining);
				remaining -= slice.bytes.length;
				if (path.equals(stream.path()))
					slice = completePhysicalLines(slice);
				if (slice.absoluteStart > 0)
					slice = alignFirstRecord(path, slice, stream.parser());
				newestFirst.add(records(slice.bytes, slice.absoluteStart, stream.parser()));
			}

			List<PhysicalRecord> matches = new ArrayList<>();
			for (int i = newestFirst.size() - 1; i >= 0; i--)
				newestFirst.get(i).stream()
						.filter(record -> LogRecordFiltering.matches(record.record, filter))
						.forEach(matches::add);

			int limit = Math.max(1, Math.min(requestedLimit, 10_000));
			List<PhysicalRecord> selected = matches.size() > limit
					? matches.subList(matches.size() - limit, matches.size())
					: matches;
			List<LogRecord> page = selected.stream().map(record -> record.record).toList();

			FileState active = state(stream.path());
			LogRecordPage response = LogRecordPage.T.create();
			response.setRecords(page);
			response.setObservedProperties(LogRecordFiltering.observedProperties(page));
			response.setMoreAvailable(matches.size() > limit);
			response.setMoreBeforeAvailable(false);
			long activeOffset = completeOffset(stream.path(), active.size);
			response.setNextCursor(cursor(stream.path(), active, activeOffset).externalForm());
			return response;
		} catch (IOException e) {
			throw new IllegalStateException("Could not read log history " + stream.descriptor().getStreamId(), e);
		}
	}

	private static long startOffset(PhysicalRecord record) {
		Long offset = record.record.getPosition().getByteOffset();
		return offset == null ? 0 : offset;
	}

	private static ReadSlice readTail(Path path, long maximumBytes) throws IOException {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		if (!compressed(name)) {
			long size = Files.size(path);
			long start = Math.max(0, size - maximumBytes);
			return read(path, start, size);
		}

		int capacity = Math.toIntExact(Math.min(MAX_READ_BYTES, maximumBytes));
		TailBytes tail = new TailBytes(capacity);
		try (InputStream content = openContent(path)) {
			byte[] buffer = new byte[64 * 1024];
			for (int read; (read = content.read(buffer)) >= 0;)
				if (read > 0)
					tail.write(buffer, 0, read);
		}
		return new ReadSlice(tail.discarded(), tail.bytes());
	}

	/**
	 * Streams every physical record from one active or rotated segment. Only one physical record is retained in memory,
	 * including its continuation lines, so canonical exports do not inherit the interactive query window's 64 MB bound.
	 */
	void forEachRecord(LogbackFileStream stream, Path path, LogFilter filter, Consumer<LogRecord> consumer) {
		CanonicalLogParser parser = stream.parser();
		if (parser == null)
			throw new IllegalArgumentException("A canonical log parser is required");

		try (InputStream input = openContent(path)) {
			long offset = 0;
			RecordBuilder current = null;
			for (byte[] bytes; (bytes = readLine(input)) != null;) {
				int contentLength = bytes.length;
				if (contentLength > 0 && bytes[contentLength - 1] == '\r')
					contentLength--;
				String text = new String(bytes, 0, contentLength, StandardCharsets.UTF_8);
				Line line = new Line(offset, offset + bytes.length + 1, text);
				offset = line.endOffset;

				if (parser.startsRecord(text)) {
					if (current != null)
						emit(current.build(parser).record, filter, consumer);
					current = new RecordBuilder(line);
				} else if (current != null) {
					current.add(line);
				} else {
					emit(parser.rawRecord(text, line.startOffset), filter, consumer);
				}
			}
			if (current != null)
				emit(current.build(parser).record, filter, consumer);
		} catch (IOException e) {
			throw new IllegalStateException("Could not stream canonical records from " + path, e);
		}
	}

	private static void emit(LogRecord record, LogFilter filter, Consumer<LogRecord> consumer) {
		if (LogRecordFiltering.matches(record, filter))
			consumer.accept(record);
	}

	private static byte[] readLine(InputStream input) throws IOException {
		ByteArrayOutputStream line = new ByteArrayOutputStream(256);
		for (int value; (value = input.read()) >= 0;) {
			if (value == '\n')
				return line.toByteArray();
			line.write(value);
		}
		return line.size() == 0 ? null : line.toByteArray();
	}

	static InputStream openContent(Path path) throws IOException {
		InputStream file = Files.newInputStream(path);
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		try {
			if (name.endsWith(".gz"))
				return new GZIPInputStream(file);
			if (name.endsWith(".zip"))
				return new ZipConcatenatingInputStream(file);
			return file;
		} catch (Throwable t) {
			file.close();
			throw t;
		}
	}

	private static boolean compressed(String name) {
		return name.endsWith(".gz") || name.endsWith(".zip");
	}

	private static FileState awaitGrowth(Path path, FileState previous, long waitMillis) throws IOException {
		long deadline = System.currentTimeMillis() + waitMillis;
		FileState current = previous;
		while (System.currentTimeMillis() < deadline) {
			try {
				Thread.sleep(Math.min(100, Math.max(1, deadline - System.currentTimeMillis())));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			current = state(path);
			if (current.generation != previous.generation || current.size > previous.size)
				break;
		}
		return current;
	}

	private static FileState state(Path path) throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
		return new FileState(attributes.creationTime().toMillis(), attributes.size());
	}

	private static ReadSlice read(Path path, long start, long end) throws IOException {
		int length = Math.toIntExact(Math.max(0, end - start));
		ByteBuffer buffer = ByteBuffer.allocate(length);
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			channel.position(start);
			while (buffer.hasRemaining()) {
				int read = channel.read(buffer);
				if (read <= 0)
					break;
			}
		}
		return new ReadSlice(start, Arrays.copyOf(buffer.array(), buffer.position()));
	}

	private static ReadSlice discardPartialFirstLine(ReadSlice slice) {
		byte[] bytes = slice.bytes;
		for (int i = 0; i < bytes.length; i++)
			if (bytes[i] == '\n')
				return new ReadSlice(slice.absoluteStart + i + 1, Arrays.copyOfRange(bytes, i + 1, bytes.length));
		return new ReadSlice(slice.absoluteStart + bytes.length, new byte[0]);
	}

	private static ReadSlice completePhysicalLines(ReadSlice slice) {
		byte[] bytes = slice.bytes;
		for (int i = bytes.length - 1; i >= 0; i--)
			if (bytes[i] == '\n')
				return new ReadSlice(slice.absoluteStart, Arrays.copyOf(bytes, i + 1));
		return new ReadSlice(slice.absoluteStart, new byte[0]);
	}

	private static ReadSlice alignFirstRecord(Path path, ReadSlice slice, CanonicalLogParser parser) throws IOException {
		ReadSlice aligned = startsAtLineBoundary(path, slice.absoluteStart) ? slice : discardPartialFirstLine(slice);
		if (!parser.recognizesRecordStarts())
			return aligned;
		for (Line line : lines(aligned.bytes, aligned.absoluteStart))
			if (parser.startsRecord(line.text)) {
				int relativeStart = Math.toIntExact(line.startOffset - aligned.absoluteStart);
				return new ReadSlice(line.startOffset, Arrays.copyOfRange(aligned.bytes, relativeStart, aligned.bytes.length));
			}
		return new ReadSlice(aligned.absoluteStart + aligned.bytes.length, new byte[0]);
	}

	private static boolean startsAtLineBoundary(Path path, long offset) throws IOException {
		if (offset <= 0 || compressed(path.getFileName().toString().toLowerCase(Locale.ROOT)))
			return offset <= 0;
		ByteBuffer previous = ByteBuffer.allocate(1);
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			channel.position(offset - 1);
			return channel.read(previous) == 1 && previous.array()[0] == '\n';
		}
	}

	private static long completeOffset(Path path, long size) throws IOException {
		if (size <= 0)
			return 0;
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			ByteBuffer last = ByteBuffer.allocate(1);
			channel.position(size - 1);
			if (channel.read(last) == 1 && last.array()[0] == '\n')
				return size;
			long start = Math.max(0, size - 64 * 1024);
			ReadSlice tail = read(path, start, size);
			for (int i = tail.bytes.length - 1; i >= 0; i--)
				if (tail.bytes[i] == '\n')
					return tail.absoluteStart + i + 1;
			return 0;
		}
	}

	private static boolean valid(Path path, FileState state, FileCursor cursor) throws IOException {
		if (cursor == null || cursor.generation != state.generation || cursor.offset < 0 || cursor.offset > state.size)
			return false;
		return cursor.anchor == null || cursor.anchor.equals(anchor(path, cursor.offset));
	}

	private static FileCursor cursor(Path path, FileState state, long offset) throws IOException {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return new FileCursor(state.generation, offset, compressed(name) ? logicalAnchor(path, offset) : anchor(path, offset));
	}

	private static String logicalAnchor(Path path, long offset) throws IOException {
		int length = Math.toIntExact(Math.min(CURSOR_ANCHOR_BYTES, Math.max(0, offset)));
		if (length == 0)
			return "0";
		byte[] bytes = new byte[length];
		try (InputStream input = openContent(path)) {
			if (!skip(input, offset - length))
				return "";
			int position = 0;
			while (position < length) {
				int read = input.read(bytes, position, length - position);
				if (read < 0)
					return "";
				position += read;
			}
		}
		CRC32 crc = new CRC32();
		crc.update(bytes);
		return Long.toUnsignedString(crc.getValue(), 16);
	}

	private static String anchor(Path path, long offset) throws IOException {
		int length = Math.toIntExact(Math.min(CURSOR_ANCHOR_BYTES, Math.max(0, offset)));
		if (length == 0)
			return "0";
		ByteBuffer buffer = ByteBuffer.allocate(length);
		try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
			channel.position(offset - length);
			while (buffer.hasRemaining() && channel.read(buffer) > 0) {
				// Keep reading the small cursor anchor until complete or EOF.
			}
		}
		CRC32 crc = new CRC32();
		crc.update(buffer.array(), 0, buffer.position());
		return Long.toUnsignedString(crc.getValue(), 16);
	}

	private static List<PhysicalRecord> records(byte[] bytes, long absoluteStart, CanonicalLogParser parser) {
		List<Line> lines = lines(bytes, absoluteStart);
		if (parser == null)
			throw new IllegalArgumentException("A canonical log parser is required");

		List<PhysicalRecord> result = new ArrayList<>();
		RecordBuilder current = null;
		for (Line line : lines) {
			if (parser.startsRecord(line.text)) {
				if (current != null)
					result.add(current.build(parser));
				current = new RecordBuilder(line);
			} else if (current != null) {
				current.add(line);
			} else {
				result.add(rawLine(line, parser));
			}
		}
		if (current != null)
			result.add(current.build(parser));
		return result;
	}

	private static PhysicalRecord rawLine(Line line, CanonicalLogParser parser) {
		LogRecord record = parser.rawRecord(line.text, line.startOffset);
		return new PhysicalRecord(record, line.endOffset);
	}

	private static List<Line> lines(byte[] bytes, long absoluteStart) {
		List<Line> result = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < bytes.length; i++) {
			if (bytes[i] != '\n')
				continue;
			result.add(line(bytes, start, i, absoluteStart, i + 1));
			start = i + 1;
		}
		if (start < bytes.length)
			result.add(line(bytes, start, bytes.length, absoluteStart, bytes.length));
		return result;
	}

	private static Line line(byte[] bytes, int start, int contentEnd, long absoluteStart, int physicalEnd) {
		int end = contentEnd;
		if (end > start && bytes[end - 1] == '\r')
			end--;
		String text = new String(bytes, start, end - start, StandardCharsets.UTF_8);
		return new Line(absoluteStart + start, absoluteStart + physicalEnd, text);
	}

	private record FileState(long generation, long size) {
	}

	private record ReadSlice(long absoluteStart, byte[] bytes) {
	}

	private record LogicalRead(byte[] bytes, boolean eof) {
	}

	private record SegmentSlice(FileState state, ReadSlice slice, boolean more) {
	}

	private record SegmentRecord(Path path, FileState state, PhysicalRecord record) {
	}

	private record SegmentProgress(Path path, FileState state, long offset) {
	}

	private record FileCursor(long generation, long offset, String anchor) {
		private String externalForm() {
			return generation + ":" + offset + ":" + anchor;
		}

		private static FileCursor parse(String cursor) {
			if (cursor == null || cursor.isBlank())
				return null;
			String[] parts = cursor.split(":", 3);
			try {
				if (parts.length == 1)
					return new FileCursor(-1, Long.parseLong(parts[0]), null);
				return new FileCursor(Long.parseLong(parts[0]), Long.parseLong(parts[1]), parts.length == 3 ? parts[2] : null);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid file log cursor: " + cursor, e);
			}
		}
	}

	private record Line(long startOffset, long endOffset, String text) {
	}

	private record PhysicalRecord(LogRecord record, long endOffset) {
	}

	private static final class TailBytes {
		private final byte[] bytes;
		private int start;
		private int size;
		private long total;

		private TailBytes(int capacity) {
			bytes = new byte[Math.max(1, capacity)];
		}

		private void write(byte[] source, int offset, int length) {
			total += length;
			for (int i = 0; i < length; i++) {
				if (size < bytes.length) {
					bytes[(start + size) % bytes.length] = source[offset + i];
					size++;
				} else {
					bytes[start] = source[offset + i];
					start = (start + 1) % bytes.length;
				}
			}
		}

		private long discarded() {
			return total - size;
		}

		private byte[] bytes() {
			byte[] result = new byte[size];
			int first = Math.min(size, bytes.length - start);
			System.arraycopy(bytes, start, result, 0, first);
			if (first < size)
				System.arraycopy(bytes, 0, result, first, size - first);
			return result;
		}
	}

	/** Presents all regular ZIP entries as one newline-separated input stream without materializing the archive. */
	private static final class ZipConcatenatingInputStream extends InputStream {
		private final ZipInputStream zip;
		private boolean separator;

		private ZipConcatenatingInputStream(InputStream input) throws IOException {
			zip = new ZipInputStream(input);
			nextEntry();
		}

		@Override
		public int read() throws IOException {
			byte[] one = new byte[1];
			int read = read(one, 0, 1);
			return read < 0 ? -1 : one[0] & 0xff;
		}

		@Override
		public int read(byte[] bytes, int offset, int length) throws IOException {
			if (separator) {
				separator = false;
				bytes[offset] = '\n';
				return 1;
			}
			int read = zip.read(bytes, offset, length);
			if (read >= 0)
				return read;
			if (!nextEntry())
				return -1;
			separator = true;
			return read(bytes, offset, length);
		}

		private boolean nextEntry() throws IOException {
			for (ZipEntry entry; (entry = zip.getNextEntry()) != null;)
				if (!entry.isDirectory())
					return true;
			return false;
		}

		@Override
		public void close() throws IOException {
			zip.close();
		}
	}

	private static final class RecordBuilder {
		private final long startOffset;
		private long endOffset;
		private final StringBuilder text;

		private RecordBuilder(Line line) {
			startOffset = line.startOffset;
			endOffset = line.endOffset;
			text = new StringBuilder(line.text);
		}

		private void add(Line line) {
			text.append('\n').append(line.text);
			endOffset = line.endOffset;
		}

		private PhysicalRecord build(CanonicalLogParser parser) {
			return new PhysicalRecord(parser.parse(text.toString(), startOffset), endOffset);
		}
	}
}
