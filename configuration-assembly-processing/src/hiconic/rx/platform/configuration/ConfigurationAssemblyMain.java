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
package hiconic.rx.platform.configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
			Path defaultResources = Files.isDirectory(applicationDirectory.resolve("packaged-resources"))
					? applicationDirectory.resolve("packaged-resources")
					: applicationDirectory.resolve("classpath-resources");
			Path resourcesDirectory = aliasedOptionPath(options, "--packaged-resources", "--classpath-resources", defaultResources);
			Path confDirectory = optionPath(options, "--conf-dir", applicationDirectory.resolve("conf"));
			Path outputDirectory = optionPath(options, "--output-dir", confDirectory);
			Path protocolFile = optionPath(options, "--protocol-file",
					applicationDirectory.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE));
			Path resourceIndexFile = optionPath(options, "--resource-index-file",
					applicationDirectory.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE));
			boolean includePackagedResourceDiagnostics = optionBoolean(options,
					"--include-packaged-resource-diagnostics", false);
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

			Maybe<Void> writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), index, outputDirectory,
					resourceIndexFile, protocolFile, artifactCoordinates(applicationDirectory.resolve("packaged-solutions.txt")));
			if (writeMaybe.isUnsatisfied()) {
				System.err.println(writeMaybe.whyUnsatisfied().stringify());
				return 1;
			}
			if (!includePackagedResourceDiagnostics)
				removePackagedResourceDiagnostics(applicationDirectory, resourcesDirectory);

			ConfigurationAssembly assembly = assemblyMaybe.get();
			System.out.println("Compiled " + assembly.configurations().size() + " modeled configuration(s) into " + outputDirectory);
			if (!assembly.report().getResidualResources().isEmpty())
				System.out.println("Materialized " + assembly.report().getResidualResources().size() + " packaged resource(s)");
			return 0;
		} catch (IllegalArgumentException e) {
			System.err.println(e.getMessage());
			System.err.println("Usage: ConfigurationAssemblyMain --application-dir <path> "
					+ "[--packaged-resources <path>] [--conf-dir <path>] [--output-dir <path>] "
					+ "[--resource-index-file <path>] [--protocol-file <path>] "
					+ "[--include-packaged-resource-diagnostics <true|false>]");
			return 2;
		}
	}

	private static boolean optionBoolean(Map<String, String> options, String name, boolean defaultValue) {
		String value = options.remove(name);
		if (value == null || value.isBlank())
			return defaultValue;
		if (value.equalsIgnoreCase("true"))
			return true;
		if (value.equalsIgnoreCase("false"))
			return false;
		throw new IllegalArgumentException("Option " + name + " must be true or false");
	}

	private static void removePackagedResourceDiagnostics(Path applicationDirectory, Path resourcesDirectory) {
		Path application = applicationDirectory.toAbsolutePath().normalize();
		Path resources = resourcesDirectory.toAbsolutePath().normalize();
		Path packagedResources = application.resolve("packaged-resources").normalize();
		Path legacyClasspathResources = application.resolve("classpath-resources").normalize();
		if (!resources.equals(packagedResources) && !resources.equals(legacyClasspathResources))
			return;
		if (!Files.isDirectory(resources))
			return;
		try (var paths = Files.walk(resources)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
				Files.delete(path);
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException("Could not remove packaged-resource diagnostic snapshot " + resources, e);
		}
	}

	private static Map<String, String> artifactCoordinates(Path packagedSolutions) {
		if (!Files.isRegularFile(packagedSolutions))
			return Map.of();
		try {
			Map<String, String> result = new LinkedHashMap<>();
			Set<String> ambiguous = new LinkedHashSet<>();
			for (String line : Files.readAllLines(packagedSolutions)) {
				String coordinate = line.strip();
				if (coordinate.isEmpty())
					continue;
				int version = coordinate.indexOf('#');
				if (version >= 0)
					coordinate = coordinate.substring(0, version);
				int separator = coordinate.indexOf(':');
				if (separator <= 0 || separator == coordinate.length() - 1)
					continue;
				String artifactId = coordinate.substring(separator + 1);
				if (ambiguous.contains(artifactId))
					continue;
				String previous = result.putIfAbsent(artifactId, coordinate);
				if (previous != null && !previous.equals(coordinate)) {
					result.remove(artifactId);
					ambiguous.add(artifactId);
				}
			}
			return Map.copyOf(result);
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException("Could not read " + packagedSolutions, e);
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

	private static Path aliasedOptionPath(Map<String, String> options, String name, String legacyName, Path defaultValue) {
		String value = options.remove(name);
		String legacyValue = options.remove(legacyName);
		if (value != null && legacyValue != null)
			throw new IllegalArgumentException("Options " + name + " and " + legacyName + " are mutually exclusive");
		String selected = value != null ? value : legacyValue;
		if (selected == null || selected.isBlank())
			return defaultValue.toAbsolutePath().normalize();
		return Path.of(selected).toAbsolutePath().normalize();
	}
}
