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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.braintribe.gm.config.assembly.model.ConfigurationAssemblyReport;
import com.braintribe.gm.config.assembly.model.ConfigurationImport;
import com.braintribe.gm.config.assembly.model.ConfigurationImports;
import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.gm.config.yaml.index.ClasspathEntry;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.config.ConfigurationError;

/**
 * Reads, validates and merges the deployment-property declarations carried by configuration artifacts.
 * <p>
 * This class intentionally knows neither Ant nor artifact resolution. Its complete input is the same indexed resource view used by the application.
 */
public final class ConfigurationImportDeclarations {

	public static final String RESOURCE_PATH = "META-INF/configuration-imports.yaml";
	public static final Set<String> PLATFORM_VARIABLES = Set.of("reflex.app.dir");

	private final Map<String, ConfigurationImport> importsByName;
	private final boolean declaredConfiguration;

	private ConfigurationImportDeclarations(Map<String, ConfigurationImport> importsByName, boolean declaredConfiguration) {
		this.importsByName = Map.copyOf(importsByName);
		this.declaredConfiguration = declaredConfiguration;
	}

	public static Maybe<ConfigurationImportDeclarations> read(ClasspathIndex classpathIndex) {
		Objects.requireNonNull(classpathIndex, "classpathIndex");

		List<DeclarationSource> sources = classpathIndex.forPrefix(RESOURCE_PATH).stream()
				.filter(entry -> entry.path.equals(RESOURCE_PATH))
				.map(ConfigurationImportDeclarations::source)
				.toList();

		return read(sources);
	}

	static Maybe<ConfigurationImportDeclarations> read(List<DeclarationSource> sources) {
		var errors = Reasons.aggregatorForceWrap(() -> ConfigurationError.create("Invalid configuration import declarations"));
		Map<String, DeclaredImport> merged = new LinkedHashMap<>();

		for (DeclarationSource source : sources) {
			Maybe<ConfigurationImports> declarationsMaybe = YamlConfigurations.read(ConfigurationImports.T).from(source.entry().url);
			if (declarationsMaybe.isUnsatisfied()) {
				errors.accept(Reasons.build(ConfigurationError.T)
						.text("Could not read configuration imports from " + source.origin())
						.cause(declarationsMaybe.whyUnsatisfied())
						.toReason());
				continue;
			}

			ConfigurationImports declarations = declarationsMaybe.get();
			for (ConfigurationImport declaration : declarations.getImports())
				merge(source.origin(), declaration, merged, errors);
		}

		Map<String, ConfigurationImport> result = new LinkedHashMap<>();
		merged.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> result.put(entry.getKey(), entry.getValue().declaration()));

		ConfigurationImportDeclarations declarations = new ConfigurationImportDeclarations(result, !sources.isEmpty());
		if (errors.hasReason())
			return Maybe.incomplete(declarations, errors.get());

		return Maybe.complete(declarations);
	}

	public List<ConfigurationImport> imports() {
		return importsByName.values().stream().sorted(Comparator.comparing(ConfigurationImport::getName)).toList();
	}

	public Set<String> names() {
		return importsByName.keySet();
	}

	/**
	 * Returns whether at least one import descriptor was present.
	 * <p>
	 * This distinction lets applications without descriptors retain the legacy property lookup during migration, while even an intentionally empty
	 * descriptor opts an application into the managed-property regime.
	 */
	public boolean declaredConfiguration() {
		return declaredConfiguration;
	}

	/**
	 * Builds the import-related part of an assembly report. An unresolved variable is legal exactly when an artifact declared it as an import.
	 */
	public ConfigurationAssemblyReport report(Collection<String> unresolvedVariables) {
		Set<String> unresolved = new LinkedHashSet<>(unresolvedVariables);
		List<String> sortedUnresolved = unresolved.stream().sorted().toList();
		List<String> platformVariables = unresolved.stream().filter(PLATFORM_VARIABLES::contains).sorted().toList();
		List<String> undeclared = unresolved.stream()
				.filter(name -> !PLATFORM_VARIABLES.contains(name))
				.filter(name -> !importsByName.containsKey(name))
				.sorted()
				.toList();

		ConfigurationAssemblyReport report = ConfigurationAssemblyReport.T.create();
		report.setImports(new ArrayList<>(imports()));
		report.setUnresolvedVariables(sortedUnresolved);
		report.setUndeclaredVariables(undeclared);
		report.setPlatformVariables(platformVariables);
		return report;
	}

	private static DeclarationSource source(ClasspathEntry entry) {
		String origin = entry.origin.isBlank() ? entry.url.toString() : entry.origin;
		return new DeclarationSource(origin, entry);
	}

	private static void merge(String origin, ConfigurationImport declaration, Map<String, DeclaredImport> merged,
			com.braintribe.gm.model.reason.ReasonAggregator<ConfigurationError> errors) {
		String name = declaration.getName();
		if (name == null || name.isBlank()) {
			errors.accept(ConfigurationError.create("Configuration import without a name in " + origin));
			return;
		}

		DeclaredImport previous = merged.get(name);
		if (previous == null) {
			merged.put(name, new DeclaredImport(origin, declaration));
			return;
		}

		if (!equivalent(previous.declaration(), declaration)) {
			errors.accept(ConfigurationError.create("Incompatible declarations for configuration import [" + name + "] in ["
					+ previous.origin() + "] and [" + origin + "]"));
		}
	}

	private static boolean equivalent(ConfigurationImport left, ConfigurationImport right) {
		return left.getRequired() == right.getRequired()
				&& left.getConfidential() == right.getConfidential()
				&& Objects.equals(left.getDescription(), right.getDescription());
	}

	record DeclarationSource(String origin, ClasspathEntry entry) {
	}

	private record DeclaredImport(String origin, ConfigurationImport declaration) {
	}
}
