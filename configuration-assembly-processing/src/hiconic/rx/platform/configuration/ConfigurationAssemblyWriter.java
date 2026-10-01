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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
import com.braintribe.gm.config.assembly.model.ArtifactResourceSection;
import com.braintribe.gm.config.assembly.model.MaterializedResource;
import com.braintribe.gm.config.assembly.model.PackagedResourceIndex;
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
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.VdHolder;
import com.braintribe.model.generic.value.ValueDescriptor;
import com.braintribe.model.bvd.resource.PackagedResource;
import com.braintribe.model.bvd.resource.PackagedResourceText;
import com.braintribe.model.bvd.resource.PackagedSource;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionCodecOption;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionProjectionOption;

import hiconic.rx.platform.conf.RxConfigurationValueDescriptorExperts;

/**
 * Writes the canonical modeled closure and validates every written entity by parsing and serializing it again.
 */
public final class ConfigurationAssemblyWriter {

	public static final String COMPILED_SLOT = "compiled";
	public static final String PROTOCOL_FILE = "configuration-compilation.yaml";
	public static final String MERGE_PROTOCOL_FILE = "configuration-merge-report.yaml";
	public static final String RESOURCE_INDEX_FILE = "packaged-resource-index.yaml";
	private static final String CLASSPATH_CONF_PREFIX = "HICONIC-CONF/";
	private static final Map<String, String> PREMERGED_WELL_KNOWN_RESOURCES = Map.of(
			"HICONIC-CONF/jvm.options", "jvm.options");

	private ConfigurationAssemblyWriter() {
	}

	/**
	 * Writes the deployable, integral configuration space. Effective modeled YAML and the files addressed by its packaged-resource expressions live
	 * together in {@code conf}. Existing non-modeled operational files in that directory are retained.
	 */
	public static Maybe<Void> writeIntegrated(ConfigurationAssembly assembly, ClasspathIndex sourceIndex, Path confDirectory,
			Path resourceIndexFile, Path protocolFile, Map<String, String> artifactCoordinates) {
		try {
			Files.createDirectories(confDirectory);
			WellKnownConfigurationCompiler.Compilation wellKnownCompilation =
					WellKnownConfigurationCompiler.compile(sourceIndex, confDirectory);
			for (String consumed : assembly.consumedResources())
				Files.deleteIfExists(confDirectory.resolve(consumed));

			List<ResourceReference> references = collectResourceReferences(assembly);
			Map<ResourceKey, ClasspathEntry> sources = locateSources(references, sourceIndex, confDirectory);
			addUnconsumedSources(assembly, sourceIndex, confDirectory, sources);
			Map<ResourceKey, String> materializedPaths = materializeResources(sources, confDirectory);
			qualifyOnlyAmbiguousReferences(references, artifactCoordinates);

			for (Map.Entry<ConfigurationKey, GenericEntity> entry : assembly.configurations().entrySet()) {
				String yaml = serialize(entry.getValue(), entry.getKey().type());
				Maybe<Void> roundtripMaybe = verifyRoundtrip(entry.getKey(), yaml);
				if (roundtripMaybe.isUnsatisfied())
					return roundtripMaybe;
				Files.writeString(confDirectory.resolve(entry.getKey().fileName()), yaml, StandardCharsets.UTF_8);
			}

			Map<String, String> effectiveProperties = effectiveProperties(assembly);
			if (!effectiveProperties.isEmpty())
				Files.writeString(confDirectory.resolve("properties.yaml"), serializeProperties(effectiveProperties), StandardCharsets.UTF_8);

			PackagedResourceIndex resourceIndex = createResourceIndex(materializedPaths, artifactCoordinates);
			Files.writeString(resourceIndexFile, serialize(resourceIndex, PackagedResourceIndex.T), StandardCharsets.UTF_8);
			writeMergeProtocol(assembly, sourceIndex, confDirectory, protocolFile.resolveSibling(MERGE_PROTOCOL_FILE), artifactCoordinates,
					wellKnownCompilation);
			assembly.report().setResidualResources(materializedPaths.values().stream().sorted().toList());
			Files.writeString(protocolFile, serialize(assembly.report(), ConfigurationAssemblyReport.T), StandardCharsets.UTF_8);
			return Maybe.complete(null);
		} catch (Exception e) {
			return Reasons.build(ConfigurationError.T)
					.text("Could not write integral assembled configuration to " + confDirectory + ": " + e.getMessage())
					.toMaybe();
		}
	}

	private static List<ResourceReference> collectResourceReferences(ConfigurationAssembly assembly) {
		List<ResourceReference> result = new ArrayList<>();
		assembly.configurations().forEach((key, configuration) ->
				collectResourceReferences(configuration, key, result, new IdentityHashMap<>()));
		return result;
	}

	private static void collectResourceReferences(Object value, ConfigurationKey configuration,
			List<ResourceReference> result, IdentityHashMap<Object, Boolean> visited) {
		if (value == null || visited.put(value, Boolean.TRUE) != null)
			return;

		if (VdHolder.isVdHolder(value)) {
			collectResourceReferences(((VdHolder) value).vd, configuration, result, visited);
			return;
		}

		if (value instanceof PackagedSource descriptor)
			result.add(reference(configuration, descriptor.getArtifact(), descriptor.getPath(), descriptor::setArtifact));
		else if (value instanceof PackagedResource descriptor)
			result.add(reference(configuration, descriptor.getArtifact(), descriptor.getPath(), descriptor::setArtifact));
		else if (value instanceof PackagedResourceText descriptor)
			result.add(reference(configuration, descriptor.getArtifact(), descriptor.getPath(), descriptor::setArtifact));

		if (value instanceof GenericEntity entity) {
			for (Property property : entity.entityType().getProperties())
				collectResourceReferences(property.getDirect(entity), configuration, result, visited);
		} else if (value instanceof Map<?, ?> map) {
			map.forEach((key, entryValue) -> {
				collectResourceReferences(key, configuration, result, visited);
				collectResourceReferences(entryValue, configuration, result, visited);
			});
		} else if (value instanceof Collection<?> collection) {
			collection.forEach(element -> collectResourceReferences(element, configuration, result, visited));
		}
	}

	private static ResourceReference reference(ConfigurationKey configuration, String artifact, String path,
			java.util.function.Consumer<String> artifactSetter) {
		if (artifact == null || artifact.isBlank())
			throw new IllegalArgumentException("Deferred packaged resource in " + configuration.displayName() + " has no owning artifact: " + path);
		String logicalPath = resolveLogicalPath(CLASSPATH_CONF_PREFIX + configuration.fileName(), path);
		return new ResourceReference(new ResourceKey(artifact, logicalPath), artifactSetter);
	}

	private static String resolveLogicalPath(String documentPath, String configuredPath) {
		if (configuredPath == null || configuredPath.isBlank())
			throw new IllegalArgumentException("A packaged resource path must not be empty");
		String candidate = configuredPath.replace('\\', '/');
		if (candidate.startsWith("./") || candidate.startsWith("../")) {
			int separator = documentPath.lastIndexOf('/');
			candidate = documentPath.substring(0, separator + 1) + candidate;
		} else {
			while (candidate.startsWith("/"))
				candidate = candidate.substring(1);
		}
		Path normalized = Paths.get(candidate).normalize();
		String result = normalized.toString().replace('\\', '/');
		if (normalized.isAbsolute() || result.isBlank() || result.equals("..") || result.startsWith("../"))
			throw new IllegalArgumentException("Packaged resource path escapes its artifact: " + configuredPath);
		return result;
	}

	private static Map<ResourceKey, ClasspathEntry> locateSources(List<ResourceReference> references, ClasspathIndex sourceIndex,
			Path confDirectory) {
		Map<ResourceKey, ClasspathEntry> result = new LinkedHashMap<>();
		for (ResourceReference reference : references) {
			ResourceKey key = reference.key();
			List<ClasspathEntry> candidates = sourceIndex.forPrefix(key.path()).stream()
					.filter(entry -> entry.path.equals(key.path()) && entry.artifactId.equals(key.artifact()))
					.toList();
			if (candidates.isEmpty() && key.artifact().equals("pipeline")) {
				Path file = confDirectory.resolve(naturalMaterializedPath(key.path())).normalize();
				if (file.startsWith(confDirectory.normalize()) && Files.isRegularFile(file))
					try {
						candidates = List.of(new ClasspathEntry(key.path(), file.toUri().toURL(), "pipeline"));
					} catch (java.net.MalformedURLException e) {
						throw new IllegalArgumentException("Cannot address pipeline resource " + file, e);
					}
			}
			if (candidates.size() != 1)
				throw new IllegalArgumentException("Expected exactly one packaged resource " + key + ", found " + candidates.size());
			result.putIfAbsent(key, candidates.get(0));
		}
		return result;
	}

	/**
	 * Adds every packaged artifact resource which was not replaced by an effective modeled configuration. This deliberately includes resources
	 * outside {@code HICONIC-CONF}: the integral application's {@code conf} directory is the one productive resource space, not merely a
	 * configuration projection.
	 */
	private static void addUnconsumedSources(ConfigurationAssembly assembly, ClasspathIndex sourceIndex, Path confDirectory,
			Map<ResourceKey, ClasspathEntry> result) {
		for (ClasspathEntry entry : sourceIndex.forPrefix("")) {
			if (entry.path == null || entry.path.isBlank())
				continue;
			if (isPremergedWellKnownResource(entry.path, confDirectory))
				continue;
			if (entry.path.startsWith(CLASSPATH_CONF_PREFIX)) {
				String relative = entry.path.substring(CLASSPATH_CONF_PREFIX.length());
				if (relative.isEmpty() || assembly.consumedResources().contains(relative))
					continue;
			}
			if (entry.artifactId == null || entry.artifactId.isBlank())
				continue;
			ResourceKey key = new ResourceKey(entry.artifactId, entry.path);
			ClasspathEntry previous = result.putIfAbsent(key, entry);
			if (previous != null && !previous.url.equals(entry.url))
				throw new IllegalArgumentException("Multiple residual resources have the same artifact and path: " + key);
		}
	}

	private static boolean isPremergedWellKnownResource(String logicalPath, Path confDirectory) {
		String materializedName = PREMERGED_WELL_KNOWN_RESOURCES.get(logicalPath);
		if (materializedName != null && Files.isRegularFile(confDirectory.resolve(materializedName)))
			return true;
		String name = logicalPath.startsWith(CLASSPATH_CONF_PREFIX)
				? logicalPath.substring(CLASSPATH_CONF_PREFIX.length())
				: logicalPath;
		return WellKnownConfigurationCompiler.isLayer(name, "log-levels", ".properties")
				&& Files.isRegularFile(confDirectory.resolve("log-levels.properties"))
				|| WellKnownConfigurationCompiler.isLayer(name, "logback", ".xml")
				&& Files.isRegularFile(confDirectory.resolve("logback.xml"));
	}

	private static void writeMergeProtocol(ConfigurationAssembly assembly, ClasspathIndex sourceIndex, Path confDirectory,
			Path target, Map<String, String> artifactCoordinates,
			WellKnownConfigurationCompiler.Compilation wellKnownCompilation) throws IOException {
		Map<String, MergeOutput> outputs = new TreeMap<>();
		for (ClasspathEntry entry : sourceIndex.forPrefix(CLASSPATH_CONF_PREFIX)) {
			if (entry.artifactId == null || entry.artifactId.isBlank())
				continue;
			String relative = entry.path.substring(CLASSPATH_CONF_PREFIX.length());
			String output = compiledOutput(relative, assembly.consumedResources());
			String operation = "compiled";
			if (output == null && isPremergedWellKnownResource(entry.path, confDirectory)) {
				output = premergedOutput(entry.path);
				operation = "merged";
			}
			if (output == null)
				continue;
			String outputPath = "conf/" + output;
			MergeOutput mergeOutput = outputs.get(outputPath);
			if (mergeOutput == null) {
				mergeOutput = new MergeOutput(operation);
				outputs.put(outputPath, mergeOutput);
			}
			mergeOutput.inputs().add(new MergeInput(coordinateOf(entry.artifactId, artifactCoordinates), entry.path));
		}
		for (WellKnownConfigurationCompiler.FilesystemInput input : wellKnownCompilation.filesystemInputs())
			outputs.computeIfAbsent("conf/" + input.output(), ignored -> new MergeOutput("merged"))
					.inputs().add(new MergeInput("pipeline", input.resource()));

		StringBuilder yaml = new StringBuilder("outputs:\n");
		for (Map.Entry<String, MergeOutput> output : outputs.entrySet()) {
			yaml.append("  ").append(yamlString(output.getKey())).append(":\n");
			yaml.append("    operation: ").append(yamlString(output.getValue().operation())).append("\n");
			yaml.append("    inputs:\n");
			for (MergeInput input : output.getValue().inputs().stream().sorted().toList()) {
				yaml.append("      - artifact: ").append(yamlString(input.artifact())).append("\n");
				yaml.append("        resource: ").append(yamlString(input.resource())).append("\n");
			}
		}
		Files.writeString(target, yaml.toString(), StandardCharsets.UTF_8);
	}

	private static String premergedOutput(String logicalPath) {
		String output = PREMERGED_WELL_KNOWN_RESOURCES.get(logicalPath);
		if (output != null)
			return output;
		String name = logicalPath.startsWith(CLASSPATH_CONF_PREFIX)
				? logicalPath.substring(CLASSPATH_CONF_PREFIX.length())
				: logicalPath;
		if (WellKnownConfigurationCompiler.isLayer(name, "log-levels", ".properties"))
			return "log-levels.properties";
		if (WellKnownConfigurationCompiler.isLayer(name, "logback", ".xml"))
			return "logback.xml";
		return null;
	}

	private static String compiledOutput(String resource, Set<String> consumedResources) {
		if (!consumedResources.contains(resource) || !resource.endsWith(".yaml"))
			return null;
		String stem = resource.substring(0, resource.length() - ".yaml".length());
		int disambiguator = stem.indexOf('.');
		return (disambiguator < 0 ? stem : stem.substring(0, disambiguator)) + ".yaml";
	}

	private static String yamlString(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private record MergeOutput(String operation, List<MergeInput> inputs) {
		private MergeOutput(String operation) {
			this(operation, new ArrayList<>());
		}
	}

	private record MergeInput(String artifact, String resource) implements Comparable<MergeInput> {
		@Override public int compareTo(MergeInput other) {
			int byArtifact = artifact.compareTo(other.artifact);
			return byArtifact != 0 ? byArtifact : resource.compareTo(other.resource);
		}
	}

	private static Map<ResourceKey, String> materializeResources(Map<ResourceKey, ClasspathEntry> sources, Path confDirectory) throws IOException {
		Map<ResourceKey, String> result = new LinkedHashMap<>();
		Set<String> occupied = new HashSet<>();
		try (var paths = Files.walk(confDirectory)) {
			paths.filter(Files::isRegularFile).forEach(path -> occupied.add(confDirectory.relativize(path).toString().replace('\\', '/')));
		}

		for (Map.Entry<ResourceKey, ClasspathEntry> source : sources.entrySet().stream()
				.sorted(Map.Entry.comparingByKey()).toList()) {
			ResourceKey key = source.getKey();
			String natural = naturalMaterializedPath(key.path());
			Path naturalTarget = confDirectory.resolve(natural).normalize();
			boolean alreadyMaterialized = sameFile(source.getValue(), naturalTarget);
			String targetPath = alreadyMaterialized || occupied.add(natural) ? natural : collisionPath(natural, key.artifact(), occupied);
			Path target = confDirectory.resolve(targetPath).normalize();
			if (!target.startsWith(confDirectory.normalize()))
				throw new IOException("Materialized resource escapes conf: " + targetPath);
			if (!alreadyMaterialized || !target.equals(naturalTarget)) {
				Files.createDirectories(target.getParent());
				try (InputStream in = source.getValue().url.openStream()) {
					Files.copy(in, target);
				}
			}
			result.put(key, targetPath);
		}
		return result;
	}

	private static boolean sameFile(ClasspathEntry source, Path target) {
		if (!"file".equals(source.url.getProtocol()) || !Files.isRegularFile(target))
			return false;
		try {
			return Files.isSameFile(Path.of(source.url.toURI()), target);
		} catch (Exception e) {
			return false;
		}
	}

	private static String naturalMaterializedPath(String logicalPath) {
		return logicalPath.startsWith(CLASSPATH_CONF_PREFIX) ? logicalPath.substring(CLASSPATH_CONF_PREFIX.length()) : logicalPath;
	}

	private static String collisionPath(String natural, String artifact, Set<String> occupied) {
		int slash = natural.lastIndexOf('/');
		String directory = slash < 0 ? "" : natural.substring(0, slash + 1);
		String file = slash < 0 ? natural : natural.substring(slash + 1);
		int dot = file.lastIndexOf('.');
		String stem = dot <= 0 ? file : file.substring(0, dot);
		String extension = dot <= 0 ? "" : file.substring(dot);
		String suffix = "--" + sanitizeSlot(artifact);
		String candidate = directory + stem + suffix + extension;
		int counter = 2;
		while (!occupied.add(candidate))
			candidate = directory + stem + suffix + "-" + counter++ + extension;
		return candidate;
	}

	private static void qualifyOnlyAmbiguousReferences(List<ResourceReference> references, Map<String, String> coordinates) {
		Map<String, Long> owners = references.stream().map(ResourceReference::key).distinct()
				.collect(java.util.stream.Collectors.groupingBy(ResourceKey::path, java.util.stream.Collectors.counting()));
		for (ResourceReference reference : references) {
			String artifact = owners.get(reference.key().path()) > 1
					? coordinateOf(reference.key().artifact(), coordinates)
					: null;
			reference.artifactSetter().accept(artifact);
		}
	}

	private static PackagedResourceIndex createResourceIndex(Map<ResourceKey, String> materialized,
			Map<String, String> coordinates) {
		PackagedResourceIndex index = PackagedResourceIndex.T.create();
		Map<String, ArtifactResourceSection> artifacts = new TreeMap<>();
		materialized.forEach((key, physical) -> {
			String coordinate = coordinateOf(key.artifact(), coordinates);
			String artifactCoordinate = coordinate;
			ArtifactResourceSection section = artifacts.computeIfAbsent(artifactCoordinate, ignored -> ArtifactResourceSection.T.create());
			MaterializedResource resource = MaterializedResource.T.create();
			resource.setPath(key.path());
			if (!physical.equals(naturalMaterializedPath(key.path())))
				resource.setMaterializedAs(physical);
			section.getResources().add(resource);
		});
		artifacts.values().forEach(section -> section.getResources().sort(Comparator.comparing(MaterializedResource::getPath)));
		index.setArtifacts(artifacts);
		return index;
	}

	private static String coordinateOf(String artifact, Map<String, String> coordinates) {
		if ("pipeline".equals(artifact))
			return artifact;
		String coordinate = coordinates.get(artifact);
		if (coordinate == null)
			throw new IllegalArgumentException("Cannot determine the full coordinate of packaged-resource artifact [" + artifact
					+ "]; its artifactId is absent or ambiguous in packaged-solutions.txt");
		return coordinate;
	}

	private record ResourceKey(String artifact, String path) implements Comparable<ResourceKey> {
		@Override public int compareTo(ResourceKey other) {
			int byPath = path.compareTo(other.path);
			return byPath != 0 ? byPath : artifact.compareTo(other.artifact);
		}
	}

	private record ResourceReference(ResourceKey key, java.util.function.Consumer<String> artifactSetter) {
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
		Map<String, String> slotsByArtifactId = new LinkedHashMap<>();
		Set<String> usedSlots = new HashSet<>();
		Map<Path, ClasspathEntry> targets = new LinkedHashMap<>();
		List<String> residual = new java.util.ArrayList<>();

		for (ClasspathEntry entry : sourceIndex.forPrefix(CLASSPATH_CONF_PREFIX)) {
			String relative = entry.path.substring(CLASSPATH_CONF_PREFIX.length());
			if (relative.isEmpty() || assembly.consumedResources().contains(relative))
				continue;

			String artifactId = entry.artifactId.isBlank() ? "classpath" : entry.artifactId;
			String slot = slotsByArtifactId.computeIfAbsent(artifactId, key -> uniqueSlot(key, usedSlots));
			Path target = effectiveDirectory.resolve(slot).resolve(relative).normalize();
			if (!target.startsWith(effectiveDirectory.resolve(slot).normalize()))
				throw new IOException("Residual configuration resource escapes its slot: " + entry.path);

			ClasspathEntry previous = targets.putIfAbsent(target, entry);
			if (previous != null)
				throw new IOException("Residual configuration collision at " + target + " between "
						+ previous.artifactId + " and " + entry.artifactId);

			Files.createDirectories(target.getParent());
			try (InputStream in = entry.url.openStream()) {
				Files.copy(in, target);
			}
			residual.add(slot + "/" + relative);
		}

		return residual.stream().sorted().toList();
	}

	private static String uniqueSlot(String artifactId, Set<String> usedSlots) {
		String base = sanitizeSlot(artifactId);
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
		Maybe<? extends GenericEntity> readMaybe = readForRoundtrip((EntityType) key.type(), yaml);
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

	private static <C extends GenericEntity> Maybe<C> readForRoundtrip(EntityType<C> type, String yaml) {
		return YamlConfigurations.read(type)
				.options(options -> options.set(ValueDescriptorExpressionCodecOption.class,
						RxConfigurationValueDescriptorExperts.expressionCodec()))
				.placeholders()
				.from(new StringReader(yaml));
	}

	private static String serialize(GenericEntity entity, EntityType<?> type) {
		GmSerializationOptions options = GmSerializationOptions.deriveDefaults()
				.inferredRootType(type)
				.outputPrettiness(OutputPrettiness.high)
				.set(PlaceholderSupport.class, true)
				.set(ValueDescriptorExpressionCodecOption.class, RxConfigurationValueDescriptorExperts.expressionCodec())
				.set(ValueDescriptorExpressionProjectionOption.class, (inferredType, value) -> {
					if (value instanceof ValueDescriptor)
						return (ValueDescriptor) value;
					return VdHolder.isVdHolder(value) ? ((VdHolder) value).vd : null;
				})
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
				.build();
		StringWriter writer = new StringWriter();
		new YamlMarshaller().marshall(writer, properties, options);
		return writer.toString();
	}
}
