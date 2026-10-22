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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import hiconic.rx.module.api.output.OutputChannel;

/**
 * Provides a dedicated process protocol stream without replacing the JVM's
 * standard streams.
 * <p>
 * This keeps intentional {@code ConsoleOutputs} separate from direct library,
 * JVM and bootstrap output. In particular, failures written to
 * {@link System#err} remain visible to the process supervisor.
 */
public final class ProcessStandardStreams {
	public static final String PROPERTY_PROTOCOL_OUTPUT = "reflex.protocol.output";
	public static final String PROPERTY_APP_DIR = "reflex.app.dir";

	private static final PrintStream PROCESS_OUT = System.out;
	private static final PrintStream PROCESS_ERR = System.err;

	private static OutputChannel protocolChannel;

	private ProcessStandardStreams() {
	}

	/** Captures the original process streams before the protocol channel is opened. */
	public static void initialize() {
		// Intentionally empty. Calling this method initializes the class.
	}

	/**
	 * Applies {@value #PROPERTY_PROTOCOL_OUTPUT}, if configured.
	 * <p>
	 * Relative paths are resolved below {@value #PROPERTY_APP_DIR}. Failure is
	 * reported on the original process error stream and leaves both standard
	 * streams untouched.
	 */
	public static synchronized void openConfiguredProtocol() {
		String configuredPath = System.getProperty(PROPERTY_PROTOCOL_OUTPUT);
		if (configuredPath == null || configuredPath.isBlank())
			return;

		Path appDirectory = Path.of(System.getProperty(PROPERTY_APP_DIR, "."));

		try {
			// A process protocol belongs to this process run. Starting with a clean
			// file avoids silently mixing records from different node incarnations.
			OutputChannel channel = OutputChannel.open(configuredPath, appDirectory, StandardCharsets.UTF_8, false, true,
					PROCESS_OUT, PROCESS_ERR);
			protocolChannel = channel;
		} catch (IOException | RuntimeException e) {
			PROCESS_ERR.println("Could not open process protocol " + configuredPath + ": " + e.getMessage());
			e.printStackTrace(PROCESS_ERR);
		}
	}

	/**
	 * @deprecated use {@link #openConfiguredProtocol()}; standard streams are no
	 *             longer redirected
	 */
	@Deprecated
	public static void redirectConfigured() {
		openConfiguredProtocol();
	}

	/** Returns the dedicated protocol stream, or the original stdout if none was configured. */
	public static synchronized PrintStream protocolOut() {
		return protocolChannel == null ? PROCESS_OUT : protocolChannel.stream();
	}

	public static PrintStream processOut() {
		return PROCESS_OUT;
	}

	public static PrintStream processErr() {
		return PROCESS_ERR;
	}

	public static synchronized Path protocolPath() {
		return protocolChannel == null ? null : protocolChannel.path();
	}

	public static synchronized void restore() {
		OutputChannel channel = protocolChannel;
		if (channel == null)
			return;

		protocolChannel = null;
		channel.close();
	}
}
