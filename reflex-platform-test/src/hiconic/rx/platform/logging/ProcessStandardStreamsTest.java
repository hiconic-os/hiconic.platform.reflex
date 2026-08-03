package hiconic.rx.platform.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class ProcessStandardStreamsTest {

	@Test
	public void redirectsBothJavaStandardStreamsAndRestoresThem() throws Exception {
		Path appDir = Files.createTempDirectory("rx-process-protocol-");
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		String previousAppDir = System.getProperty(ProcessStandardStreams.PROPERTY_APP_DIR);
		String previousOutput = System.getProperty(ProcessStandardStreams.PROPERTY_PROTOCOL_OUTPUT);

		try {
			System.setProperty(ProcessStandardStreams.PROPERTY_APP_DIR, appDir.toString());
			System.setProperty(ProcessStandardStreams.PROPERTY_PROTOCOL_OUTPUT, "logs/protocol.log");

			ProcessStandardStreams.redirectConfigured();
			System.out.println("stdout protocol");
			System.err.println("stderr protocol");
			ProcessStandardStreams.restore();

			assertThat(System.out).isSameAs(originalOut);
			assertThat(System.err).isSameAs(originalErr);
			assertThat(ProcessStandardStreams.protocolPath()).isNull();
			assertThat(Files.readString(appDir.resolve("logs/protocol.log")))
					.contains("stdout protocol")
					.contains("stderr protocol");
		} finally {
			ProcessStandardStreams.restore();
			restoreProperty(ProcessStandardStreams.PROPERTY_APP_DIR, previousAppDir);
			restoreProperty(ProcessStandardStreams.PROPERTY_PROTOCOL_OUTPUT, previousOutput);
			try (var paths = Files.walk(appDir)) {
				for (Path path : (Iterable<Path>) paths.sorted(java.util.Comparator.reverseOrder())::iterator)
					Files.deleteIfExists(path);
			}
		}
	}

	private static void restoreProperty(String name, String value) {
		if (value == null)
			System.clearProperty(name);
		else
			System.setProperty(name, value);
	}
}
