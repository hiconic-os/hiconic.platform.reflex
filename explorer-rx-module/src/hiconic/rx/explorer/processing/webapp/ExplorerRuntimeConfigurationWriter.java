// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.explorer.processing.webapp;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.braintribe.cfg.Required;

/**
 * Materializes the deployment-specific JSON configuration consumed by the Explorer web application.
 */
public class ExplorerRuntimeConfigurationWriter {

	public static final String RUNTIME_CONFIGURATION_FILE = "runtime-config.json";

	private Path webAppRoot;
	private Map<String, String> runtimeProperties = new LinkedHashMap<>();

	@Required
	public void setWebAppRoot(Path webAppRoot) {
		this.webAppRoot = webAppRoot;
	}

	@Required
	public void setRuntimeProperties(Map<String, String> runtimeProperties) {
		this.runtimeProperties = new LinkedHashMap<>(runtimeProperties);
	}

	public void write() {
		try {
			writeAtomically(webAppRoot.resolve(RUNTIME_CONFIGURATION_FILE), renderRuntimeConfiguration());
		} catch (IOException e) {
			throw new IllegalStateException("Could not configure Explorer web application in " + webAppRoot, e);
		}
	}

	private String renderRuntimeConfiguration() {
		StringBuilder result = new StringBuilder();
		result.append("{\n");

		boolean first = true;
		for (Map.Entry<String, String> entry : runtimeProperties.entrySet()) {
			if (entry.getValue() == null)
				continue;

			if (!first)
				result.append(",\n");
			result.append("  \"").append(escapeJson(entry.getKey())).append("\": \"")
					.append(escapeJson(entry.getValue())).append('"');
			first = false;
		}

		result.append("\n}\n");
		return result.toString();
	}

	private static String escapeJson(String value) {
		StringBuilder result = new StringBuilder(value.length() + 16);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '\\' -> result.append("\\\\");
				case '"' -> result.append("\\\"");
				case '\b' -> result.append("\\b");
				case '\f' -> result.append("\\f");
				case '\n' -> result.append("\\n");
				case '\r' -> result.append("\\r");
				case '\t' -> result.append("\\t");
				case '<' -> result.append("\\u003c");
				case '>' -> result.append("\\u003e");
				case '&' -> result.append("\\u0026");
				default -> {
					if (c < 0x20) {
						result.append("\\u00");
						result.append(Character.forDigit((c >> 4) & 0xf, 16));
						result.append(Character.forDigit(c & 0xf, 16));
					} else {
						result.append(c);
					}
				}
			}
		}
		return result.toString();
	}

	private static void writeAtomically(Path target, String content) throws IOException {
		Path temporaryFile = target.resolveSibling(target.getFileName() + ".tmp");
		Files.writeString(temporaryFile, content, UTF_8);
		try {
			Files.move(temporaryFile, target, ATOMIC_MOVE, REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temporaryFile, target, REPLACE_EXISTING);
		}
	}
}
