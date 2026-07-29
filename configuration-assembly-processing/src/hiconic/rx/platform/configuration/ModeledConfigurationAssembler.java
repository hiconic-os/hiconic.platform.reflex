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

import static com.braintribe.utils.lcd.StringTools.camelCaseToSocialDistancingCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.config.assembly.model.ConfigurationAssemblyReport;
import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.config.yaml.api.PartiallyResolvedConfiguration;
import com.braintribe.gm.config.yaml.index.ClasspathEntry;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Model;
import com.braintribe.model.meta.GmMetaModel;

import hiconic.rx.platform.loading.RxPropertiesLoader;

/**
 * Builds and validates the static, modeled configuration closure of an application.
 * <p>
 * It intentionally reuses the runtime loaders, ordering and merge implementation. Programmatic configuration registrations are outside this static
 * phase and therefore cannot accidentally initialize application wiring during a build.
 */
public final class ModeledConfigurationAssembler {

	public static final String DEFAULT_CLASSPATH_CONF_PATH = "HICONIC-CONF/";
	private static final Set<String> INFRASTRUCTURE_YAML = Set.of("properties");

	private final ClasspathIndex classpathIndex;
	private final File configFolder;
	private final String classpathConfPath;
	private final Collection<EntityType<?>> candidateTypes;

	public ModeledConfigurationAssembler(ClasspathIndex classpathIndex, File configFolder) {
		this(classpathIndex, configFolder, DEFAULT_CLASSPATH_CONF_PATH, packagedEntityTypes());
	}

	/** Constructor with an explicit type universe, primarily useful for deterministic isolated tests. */
	public ModeledConfigurationAssembler(ClasspathIndex classpathIndex, File configFolder, String classpathConfPath,
			Collection<EntityType<?>> candidateTypes) {
		this.classpathIndex = Objects.requireNonNull(classpathIndex, "classpathIndex");
		this.configFolder = Objects.requireNonNull(configFolder, "configFolder");
		this.classpathConfPath = normalizeClasspathPath(classpathConfPath);
		this.candidateTypes = List.copyOf(candidateTypes);
	}

	public Maybe<ConfigurationAssembly> assemble() {
		var errors = Reasons.aggregatorForceWrap(() -> ConfigurationError.create("Could not assemble modeled application configuration"));

		Maybe<Map<String, String>> rawPropertiesMaybe = RxPropertiesLoader.loadLayered(configFolder, classpathConfPath, "properties",
				new YamlMarshaller(), classpathIndex);
		if (rawPropertiesMaybe.isUnsatisfied())
			return rawPropertiesMaybe.whyUnsatisfied().asMaybe();

		Maybe<SymbolicConfigurationProperties> symbolicPropertiesMaybe = SymbolicConfigurationProperties.analyze(rawPropertiesMaybe.get());
		if (symbolicPropertiesMaybe.isUnsatisfied())
			errors.accept(symbolicPropertiesMaybe.whyUnsatisfied());
		SymbolicConfigurationProperties symbolicProperties = symbolicPropertiesMaybe.value();
		if (symbolicProperties == null)
			return errors.get().asMaybe();

		Maybe<ConfigurationImportDeclarations> declarationsMaybe = ConfigurationImportDeclarations.read(classpathIndex);
		if (declarationsMaybe.isUnsatisfied())
			errors.accept(declarationsMaybe.whyUnsatisfied());
		ConfigurationImportDeclarations declarations = declarationsMaybe.value();
		if (declarations == null)
			return errors.get().asMaybe();

		Map<String, List<EntityType<?>>> typesByBaseName = indexTypes(candidateTypes);
		Discovery discovery = discover(typesByBaseName, errors);

		ModeledYamlConfiguration loader = new ModeledYamlConfiguration();
		loader.setClasspathIndex(classpathIndex);
		loader.setClasspathConfPath(classpathConfPath);
		loader.setConfigFolder(configFolder);
		loader.setExternalReasonedPropertyLookup(symbolicProperties::resolveKnown);

		Map<ConfigurationKey, GenericEntity> configurations = new LinkedHashMap<>();
		Set<String> unresolvedAliases = new LinkedHashSet<>();

		for (ConfigurationKey key : discovery.keys()) {
			Maybe<? extends PartiallyResolvedConfiguration<? extends GenericEntity>> configMaybe =
					load(loader, key);
			if (configMaybe.isUnsatisfied()) {
				errors.accept(configMaybe.whyUnsatisfied());
				continue;
			}

			PartiallyResolvedConfiguration<? extends GenericEntity> partial = configMaybe.get();
			configurations.put(key, partial.configuration());
			unresolvedAliases.addAll(partial.unresolvedVariables());
		}

		Set<String> externalLeaves = symbolicProperties.externalLeaves(unresolvedAliases);
		ConfigurationAssemblyReport report = declarations.report(externalLeaves);
		report.setAssembledConfigurations(configurations.keySet().stream().sorted().map(ConfigurationKey::displayName).toList());
		report.setResidualResources(discovery.residualResources());

		if (!report.getUndeclaredVariables().isEmpty())
			errors.accept(ConfigurationError.create("Undeclared configuration imports: "
					+ String.join(", ", report.getUndeclaredVariables())));

		ConfigurationAssembly assembly = new ConfigurationAssembly(configurations, symbolicProperties.rawProperties(),
				symbolicProperties.resolvedProperties(), discovery.consumedResources(), report);
		if (errors.hasReason())
			return Maybe.incomplete(assembly, errors.get());

		return Maybe.complete(assembly);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static Maybe<? extends PartiallyResolvedConfiguration<? extends GenericEntity>> load(ModeledYamlConfiguration loader,
			ConfigurationKey key) {
		return loader.staticConfigPartiallyReasoned((EntityType) key.type(), key.useCase());
	}

	private Discovery discover(Map<String, List<EntityType<?>>> typesByBaseName,
			com.braintribe.gm.model.reason.ReasonAggregator<ConfigurationError> errors) {
		Set<String> resourceNames = new LinkedHashSet<>();
		String prefix = classpathConfPath;
		for (ClasspathEntry entry : classpathIndex.forPrefix(prefix)) {
			String relative = entry.path.substring(prefix.length());
			if (!relative.contains("/"))
				resourceNames.add(relative);
		}

		if (configFolder.isDirectory()) {
			File[] files = configFolder.listFiles(File::isFile);
			if (files != null)
				for (File file : files)
					resourceNames.add(file.getName());
		}

		Set<ConfigurationKey> keys = new LinkedHashSet<>();
		Set<String> consumed = new LinkedHashSet<>();
		List<String> residual = new ArrayList<>();
		for (String resourceName : resourceNames.stream().sorted().toList()) {
			if (!resourceName.endsWith(".yaml"))
				continue;

			ParsedName parsed = parse(resourceName, errors);
			if (parsed == null)
				continue;
			if (INFRASTRUCTURE_YAML.contains(parsed.baseName())) {
				consumed.add(resourceName);
				continue;
			}

			List<EntityType<?>> matches = typesByBaseName.get(parsed.baseName());
			if (matches == null || matches.isEmpty()) {
				residual.add(resourceName);
				continue;
			}

			if (matches.size() > 1) {
				errors.accept(ConfigurationError.create("Ambiguous modeled configuration resource [" + resourceName + "] matches "
						+ matches.stream().map(EntityType::getTypeSignature).sorted().toList()));
				continue;
			}

			keys.add(new ConfigurationKey(matches.get(0), parsed.useCase(), parsed.baseName()));
			consumed.add(resourceName);
		}

		return new Discovery(keys.stream().sorted().toList(), Set.copyOf(consumed), residual.stream().sorted().toList());
	}

	private static ParsedName parse(String resourceName,
			com.braintribe.gm.model.reason.ReasonAggregator<ConfigurationError> errors) {
		String stem = resourceName.substring(0, resourceName.length() - ".yaml".length());
		int disambiguator = stem.indexOf('.');
		String identity = disambiguator < 0 ? stem : stem.substring(0, disambiguator);
		int useCaseSeparator = identity.indexOf('~');
		String baseName = useCaseSeparator < 0 ? identity : identity.substring(0, useCaseSeparator);
		String useCase = useCaseSeparator < 0 ? "" : identity.substring(useCaseSeparator + 1);
		boolean malformed = baseName.isBlank()
				|| useCaseSeparator >= 0 && (useCase.isBlank() || useCase.indexOf('~') >= 0);
		if (malformed) {
			errors.accept(ConfigurationError.create("Malformed modeled configuration resource name [" + resourceName + "]"));
			return null;
		}
		return new ParsedName(baseName, useCase);
	}

	private static Map<String, List<EntityType<?>>> indexTypes(Collection<EntityType<?>> types) {
		Map<String, List<EntityType<?>>> result = new LinkedHashMap<>();
		types.stream().sorted(Comparator.comparing(EntityType::getTypeSignature)).forEach(type -> {
			String baseName = camelCaseToSocialDistancingCase(type.getShortName()).toLowerCase(Locale.ROOT);
			result.computeIfAbsent(baseName, ignored -> new ArrayList<>()).add(type);
		});
		return result;
	}

	private static Collection<EntityType<?>> packagedEntityTypes() {
		Set<EntityType<?>> result = new LinkedHashSet<>();
		for (Model model : GMF.getTypeReflection().getPackagedModels()) {
			GmMetaModel metaModel = model.getMetaModel();
			metaModel.entityTypes()
					.map(type -> GMF.getTypeReflection().findEntityType(type.getTypeSignature()))
					.filter(Objects::nonNull)
					.forEach(result::add);
		}
		return result;
	}

	private static String normalizeClasspathPath(String path) {
		String result = Objects.requireNonNull(path, "classpathConfPath").replace('\\', '/');
		while (result.startsWith("/"))
			result = result.substring(1);
		return result.endsWith("/") ? result : result + "/";
	}

	private record ParsedName(String baseName, String useCase) {
	}

	private record Discovery(List<ConfigurationKey> keys, Set<String> consumedResources, List<String> residualResources) {
	}
}
