// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2026
//
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
package hiconic.rx.platform.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.Maybe;

/**
 * Narrow application-classpath launcher for static configuration assembly.
 */
public final class ConfigurationAssemblyMain {

	private ConfigurationAssemblyMain() {
	}

	public static void main(String[] args) {
		int status = run(args);
		if (status != 0)
			System.exit(status);
	}

	static int run(String[] args) {
		try {
			Map<String, String> options = parse(args);
			Path applicationDirectory = requiredPath(options, "--application-dir");
			Path resourcesDirectory = optionPath(options, "--classpath-resources",
					applicationDirectory.resolve("classpath-resources"));
			Path outputDirectory = optionPath(options, "--output-dir", applicationDirectory.resolve("packaged-conf"));
			Path confDirectory = optionPath(options, "--conf-dir", applicationDirectory.resolve("conf"));
			if (!options.isEmpty())
				throw new IllegalArgumentException("Unknown configuration assembly option(s): " + String.join(", ", options.keySet()));

			ClasspathIndex index = Files.isDirectory(resourcesDirectory)
					? new ClasspathIndex(resourcesDirectory)
					: new ClasspathIndex();

			Maybe<ConfigurationAssembly> assemblyMaybe =
					new ModeledConfigurationAssembler(index, confDirectory.toFile()).assemble();
			if (assemblyMaybe.isUnsatisfied()) {
				System.err.println(assemblyMaybe.whyUnsatisfied().stringify());
				return 1;
			}

			Maybe<Void> writeMaybe = ConfigurationAssemblyWriter.write(assemblyMaybe.get(), outputDirectory);
			if (writeMaybe.isUnsatisfied()) {
				System.err.println(writeMaybe.whyUnsatisfied().stringify());
				return 1;
			}

			ConfigurationAssembly assembly = assemblyMaybe.get();
			System.out.println("Assembled " + assembly.configurations().size() + " modeled configuration(s) into " + outputDirectory);
			if (!assembly.report().getResidualResources().isEmpty())
				System.out.println("Retained " + assembly.report().getResidualResources().size() + " residual configuration resource(s)");
			return 0;
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: ConfigurationAssemblyMain --application-dir <path> "
					+ "[--classpath-resources <path>] [--conf-dir <path>] [--output-dir <path>]");
			return 2;
		}
	}

	private static Map<String, String> parse(String[] args) {
		if (args.length % 2 != 0)
			throw new IllegalArgumentException("Every configuration assembly option requires a value");

		Map<String, String> result = new LinkedHashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			String name = args[i];
			if (!name.startsWith("--"))
				throw new IllegalArgumentException("Unexpected configuration assembly argument: " + name);
			if (result.put(name, args[i + 1]) != null)
				throw new IllegalArgumentException("Duplicate configuration assembly option: " + name);
		}
		return result;
	}

	private static Path requiredPath(Map<String, String> options, String name) {
		String value = options.remove(name);
		if (value == null || value.isBlank())
			throw new IllegalArgumentException("Missing required configuration assembly option " + name);
		return Path.of(value).toAbsolutePath().normalize();
	}

	private static Path optionPath(Map<String, String> options, String name, Path defaultValue) {
		String value = options.remove(name);
		if (value == null || value.isBlank())
			return defaultValue.toAbsolutePath().normalize();
		return Path.of(value).toAbsolutePath().normalize();
	}
}
