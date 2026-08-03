// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.module.api.output;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * An opened process output destination with explicit ownership.
 * <p>
 * Closing a channel backed by stdout or stderr is intentionally a no-op.
 */
public final class OutputChannel implements AutoCloseable {
	private final PrintStream stream;
	private final Path path;
	private final boolean owned;

	private OutputChannel(PrintStream stream, Path path, boolean owned) {
		this.stream = stream;
		this.path = path;
		this.owned = owned;
	}

	public static OutputChannel open(String destination, Path baseDirectory, Charset charset, boolean append,
			boolean createParentDirectories, PrintStream stdout, PrintStream stderr) throws IOException {
		Objects.requireNonNull(destination, "destination");
		Objects.requireNonNull(charset, "charset");
		Objects.requireNonNull(stdout, "stdout");
		Objects.requireNonNull(stderr, "stderr");

		return switch (destination) {
			case OutputChannels.STDOUT -> borrowed(stdout);
			case OutputChannels.STDERR -> borrowed(stderr);
			case OutputChannels.NONE -> owned(new PrintStream(OutputStream.nullOutputStream(), true, charset), null);
			default -> file(destination, baseDirectory, charset, append, createParentDirectories);
		};
	}

	private static OutputChannel file(String destination, Path baseDirectory, Charset charset, boolean append,
			boolean createParentDirectories) throws IOException {
		Path path = Path.of(destination);
		if (!path.isAbsolute() && baseDirectory != null)
			path = baseDirectory.resolve(path);
		path = path.toAbsolutePath().normalize();

		Path parent = path.getParent();
		if (createParentDirectories && parent != null)
			Files.createDirectories(parent);

		OutputStream output = append
				? Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
				: Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		PrintStream stream = new PrintStream(new BufferedOutputStream(output), true, charset);
		return owned(stream, path);
	}

	private static OutputChannel borrowed(PrintStream stream) {
		return new OutputChannel(stream, null, false);
	}

	private static OutputChannel owned(PrintStream stream, Path path) {
		return new OutputChannel(stream, path, true);
	}

	public PrintStream stream() {
		return stream;
	}

	public Path path() {
		return path;
	}

	public boolean owned() {
		return owned;
	}

	@Override
	public void close() {
		if (owned)
			stream.close();
	}
}
