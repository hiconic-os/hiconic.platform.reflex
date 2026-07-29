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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.api.PlaceholderSupport;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.config.assembly.model.ConfigurationAssemblyReport;
import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.gm.config.yaml.index.ClasspathEntry;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EssentialTypes;
import com.braintribe.model.generic.reflection.MapType;
import com.braintribe.model.generic.GMF;

/**
 * Writes the canonical modeled closure and validates every written entity by parsing and serializing it again.
 */
public final class ConfigurationAssemblyWriter {

	public static final String COMPILED_SLOT = "compiled";
	public static final String PROTOCOL_FILE = "configuration-compilation.yaml";
	private static final String CLASSPATH_CONF_PREFIX = "HICONIC-CONF/";

	private ConfigurationAssemblyWriter() {
	}

	public static Maybe<Void> write(ConfigurationAssembly assembly, ClasspathIndex sourceIndex, Path effectiveDirectory, Path protocolFile) {
		try {
			recreateDirectory(effectiveDirectory);
			Path compiledDirectory = effectiveDirectory.resolve(COMPILED_SLOT);
			Files.createDirectories(compiledDirectory);

			for (Map.Entry<ConfigurationKey, GenericEntity> entry : assembly.configurations().entrySet()) {
				String yaml = serialize(entry.getValue(), entry.getKey().type());
				Maybe<Void> roundtripMaybe = verifyRoundtrip(entry.getKey(), yaml);
				if (roundtripMaybe.isUnsatisfied())
					return roundtripMaybe;
				Files.writeString(compiledDirectory.resolve(entry.getKey().fileName()), yaml, StandardCharsets.UTF_8);
			}

			Map<String, String> effectiveProperties = effectiveProperties(assembly);
			if (!effectiveProperties.isEmpty())
				Files.writeString(compiledDirectory.resolve("properties.yaml"), serializeProperties(effectiveProperties), StandardCharsets.UTF_8);

			List<String> residualResources = copyResidualResources(assembly, sourceIndex, effectiveDirectory);
			assembly.report().setResidualResources(residualResources);
			String reportYaml = serialize(assembly.report(), ConfigurationAssemblyReport.T);
			Files.writeString(protocolFile, reportYaml, StandardCharsets.UTF_8);
			return Maybe.complete(null);
		} catch (IOException | UncheckedIOException e) {
			return Reasons.build(ConfigurationError.T)
					.text("Could not write assembled configuration to " + effectiveDirectory + ": " + e)
					.toMaybe();
		}
	}

	private static Map<String, String> effectiveProperties(ConfigurationAssembly assembly) {
		Map<String, String> result = new TreeMap<>();
		assembly.rawProperties().forEach((name, rawValue) ->
				result.put(name, assembly.resolvedProperties().getOrDefault(name, rawValue)));
		return result;
	}

	private static List<String> copyResidualResources(ConfigurationAssembly assembly, ClasspathIndex sourceIndex, Path effectiveDirectory)
			throws IOException {
		Map<String, String> slotsByOrigin = new LinkedHashMap<>();
		Set<String> usedSlots = new HashSet<>();
		Map<Path, ClasspathEntry> targets = new LinkedHashMap<>();
		List<String> residual = new java.util.ArrayList<>();

		for (ClasspathEntry entry : sourceIndex.forPrefix(CLASSPATH_CONF_PREFIX)) {
			String relative = entry.path.substring(CLASSPATH_CONF_PREFIX.length());
			if (relative.isEmpty() || assembly.consumedResources().contains(relative))
				continue;

			String origin = entry.origin.isBlank() ? "classpath" : entry.origin;
			String slot = slotsByOrigin.computeIfAbsent(origin, key -> uniqueSlot(key, usedSlots));
			Path target = effectiveDirectory.resolve(slot).resolve(relative).normalize();
			if (!target.startsWith(effectiveDirectory.resolve(slot).normalize()))
				throw new IOException("Residual configuration resource escapes its slot: " + entry.path);

			ClasspathEntry previous = targets.putIfAbsent(target, entry);
			if (previous != null)
				throw new IOException("Residual configuration collision at " + target + " between "
						+ previous.origin + " and " + entry.origin);

			Files.createDirectories(target.getParent());
			try (InputStream in = entry.url.openStream()) {
				Files.copy(in, target);
			}
			residual.add(slot + "/" + relative);
		}

		return residual.stream().sorted().toList();
	}

	private static String uniqueSlot(String origin, Set<String> usedSlots) {
		String base = sanitizeSlot(origin);
		String candidate = base;
		int suffix = 2;
		while (!usedSlots.add(candidate))
			candidate = base + "-" + suffix++;
		return candidate;
	}

	private static String sanitizeSlot(String value) {
		String result = value.replaceAll("[^A-Za-z0-9._-]", "_");
		return result.isEmpty() ? "artifact" : result;
	}

	private static void recreateDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			try (var paths = Files.walk(directory)) {
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
					Files.delete(path);
			}
		}
		Files.createDirectories(directory);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Maybe<Void> verifyRoundtrip(ConfigurationKey key, String yaml) {
		Maybe<? extends GenericEntity> readMaybe = YamlConfigurations.read((EntityType) key.type())
				.placeholders()
				.from(new StringReader(yaml));
		if (readMaybe.isUnsatisfied())
			return Reasons.build(ConfigurationError.T)
					.text("Could not parse assembled configuration " + key.displayName())
					.cause(readMaybe.whyUnsatisfied())
					.toMaybe();

		String repeated = serialize(readMaybe.get(), key.type());
		if (!yaml.equals(repeated))
			return ConfigurationError.create("Configuration serialization is not stable for " + key.displayName()).asMaybe();

		return Maybe.complete(null);
	}

	private static String serialize(GenericEntity entity, EntityType<?> type) {
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults()
				.inferredRootType(type)
				.outputPrettiness(OutputPrettiness.high)
				.set(PlaceholderSupport.class, true)
				.set(TypeExplicitnessOption.class, TypeExplicitness.polymorphic)
				.build();

		StringWriter writer = new StringWriter();
		new YamlMarshaller().marshall(writer, entity, options);
		return writer.toString();
	}

	private static String serializeProperties(Map<String, String> properties) {
		MapType type = GMF.getTypeReflection().getMapType(EssentialTypes.TYPE_STRING, EssentialTypes.TYPE_STRING);
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults()
				.inferredRootType(type)
				.outputPrettiness(OutputPrettiness.high)
				.set(PlaceholderSupport.class, true)
				.build();
		StringWriter writer = new StringWriter();
		new YamlMarshaller().marshall(writer, properties, options);
		return writer.toString();
	}
}
