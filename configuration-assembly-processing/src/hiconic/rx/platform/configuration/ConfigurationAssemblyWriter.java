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
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;

import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.api.PlaceholderSupport;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.config.assembly.model.ConfigurationAssemblyReport;
import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;

/**
 * Writes the canonical modeled closure and validates every written entity by parsing and serializing it again.
 */
public final class ConfigurationAssemblyWriter {

	public static final String EFFECTIVE_DIRECTORY = "effective";
	public static final String REPORT_FILE = "assembly-report.yaml";

	private ConfigurationAssemblyWriter() {
	}

	public static Maybe<Void> write(ConfigurationAssembly assembly, Path packagedConfDirectory) {
		Path effectiveDirectory = packagedConfDirectory.resolve(EFFECTIVE_DIRECTORY);
		try {
			recreateDirectory(effectiveDirectory);

			for (Map.Entry<ConfigurationKey, GenericEntity> entry : assembly.configurations().entrySet()) {
				String yaml = serialize(entry.getValue(), entry.getKey().type());
				Maybe<Void> roundtripMaybe = verifyRoundtrip(entry.getKey(), yaml);
				if (roundtripMaybe.isUnsatisfied())
					return roundtripMaybe;
				Files.writeString(effectiveDirectory.resolve(entry.getKey().fileName()), yaml, StandardCharsets.UTF_8);
			}

			String reportYaml = serialize(assembly.report(), ConfigurationAssemblyReport.T);
			Files.writeString(packagedConfDirectory.resolve(REPORT_FILE), reportYaml, StandardCharsets.UTF_8);
			return Maybe.complete(null);
		} catch (IOException | UncheckedIOException e) {
			return Reasons.build(ConfigurationError.T)
					.text("Could not write assembled configuration to " + packagedConfDirectory + ": " + e)
					.toMaybe();
		}
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
}
