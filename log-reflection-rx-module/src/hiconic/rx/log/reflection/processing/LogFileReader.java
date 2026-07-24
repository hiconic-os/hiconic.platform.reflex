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
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import hiconic.rx.log.reflection.model.LogFilter;
import hiconic.rx.log.reflection.model.LogRecord;
import hiconic.rx.log.reflection.model.api.LogRecordPage;

/** Reads active UTF-8 log files and turns physical records into canonical records. */
public class LogFileReader {
	private static final long MAX_READ_BYTES = 64L * 1024 * 1024;

	public LogRecordPage query(LogbackFileStream stream, LogFilter filter, String cursor, int requestedLimit, long waitMillis,
			boolean includeRotated) {
		if (includeRotated && (cursor == null || cursor.isBlank()) && stream.segmentPaths().size() > 1)
			return queryHistory(stream, filter, requestedLimit);
		try {
			Path path = stream.path();
			FileState state = state(path);
			FileCursor parsedCursor = FileCursor.parse(cursor);
			long offset = parsedCursor == null || parsedCursor.generation != state.generation ? 0 : parsedCursor.offset;
			if (offset > state.size)
				offset = 0;

			if (cursor != null && offset >= state.size && waitMillis > 0)
				state = awaitGrowth(path, state, Math.min(waitMillis, 30_000));
			if (parsedCursor != null && parsedCursor.generation != state.generation)
				offset = 0;

			long readStart = offset;
			if (cursor == null && state.size - readStart > MAX_READ_BYTES)
				readStart = state.size - MAX_READ_BYTES;

			ReadSlice slice = read(path, readStart, state.size);
			if (readStart > 0 && cursor == null)
				slice = discardPartialFirstLine(slice);

			List<PhysicalRecord> physicalRecords = records(slice.bytes, slice.absoluteStart, stream.parser());
			List<PhysicalRecord> matches = physicalRecords.stream()
					.filter(record -> LogRecordFiltering.matches(record.record, filter))
					.toList();

			int limit = Math.max(1, Math.min(requestedLimit, 10_000));
			boolean initial = cursor == null;
			boolean moreAvailable = !initial && matches.size() > limit;
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

			long nextOffset;
			if (moreAvailable)
				nextOffset = selected.get(selected.size() - 1).endOffset;
			else
				nextOffset = state.size;
			response.setNextCursor(new FileCursor(state.generation, nextOffset).externalForm());
			return response;
		} catch (IOException e) {
			throw new IllegalStateException("Could not read log stream " + stream.descriptor().getStreamId(), e);
		}
	}

	private LogRecordPage queryHistory(LogbackFileStream stream, LogFilter filter, int requestedLimit) {
		try {
			List<List<PhysicalRecord>> newestFirst = new ArrayList<>();
			long remaining = MAX_READ_BYTES;
			List<Path> paths = stream.segmentPaths();
			for (int i = paths.size() - 1; i >= 0 && remaining > 0; i--) {
				ReadSlice slice = readTail(paths.get(i), remaining);
				remaining -= slice.bytes.length;
				if (slice.absoluteStart > 0)
					slice = discardPartialFirstLine(slice);
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
			response.setNextCursor(new FileCursor(active.generation, active.size).externalForm());
			return response;
		} catch (IOException e) {
			throw new IllegalStateException("Could not read log history " + stream.descriptor().getStreamId(), e);
		}
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

	private static InputStream openContent(Path path) throws IOException {
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

	private record FileCursor(long generation, long offset) {
		private String externalForm() {
			return generation + ":" + offset;
		}

		private static FileCursor parse(String cursor) {
			if (cursor == null || cursor.isBlank())
				return null;
			int colon = cursor.indexOf(':');
			try {
				if (colon < 0)
					return new FileCursor(-1, Long.parseLong(cursor));
				return new FileCursor(Long.parseLong(cursor.substring(0, colon)), Long.parseLong(cursor.substring(colon + 1)));
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
