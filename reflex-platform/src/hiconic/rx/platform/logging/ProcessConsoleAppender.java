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
package hiconic.rx.platform.logging;

import java.io.IOException;
import java.io.OutputStream;

import ch.qos.logback.core.OutputStreamAppender;

/**
 * Logback appender that always targets the process' original stdout.
 * <p>
 * Unlike {@code ConsoleAppender}, it is deliberately unaffected when the
 * application's direct {@code System.out}/{@code System.err} protocol is
 * redirected.
 */
public class ProcessConsoleAppender<E> extends OutputStreamAppender<E> {

	@Override
	public void start() {
		setOutputStream(new NonClosingOutputStream(ProcessStandardStreams.processOut()));
		super.start();
	}

	private static class NonClosingOutputStream extends OutputStream {
		private final OutputStream delegate;

		private NonClosingOutputStream(OutputStream delegate) {
			this.delegate = delegate;
		}

		@Override
		public void write(int value) throws IOException {
			delegate.write(value);
		}

		@Override
		public void write(byte[] bytes, int offset, int length) throws IOException {
			delegate.write(bytes, offset, length);
		}

		@Override
		public void flush() throws IOException {
			delegate.flush();
		}

		@Override
		public void close() throws IOException {
			delegate.flush();
		}
	}
}
