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

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.braintribe.gm.config.assembly.model.ConfigurationAssemblyReport;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;

import hiconic.rx.platform.configuration.model.SampleConfiguration;

public class ConfigurationImportDeclarationsTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void mergesEquivalentDeclarationsAndReportsUndeclaredVariables() throws Exception {
		Path root = temporaryFolder.newFolder("mirror").toPath();
		artifact(root, "base", """
				imports:
				  - name: DB_HOST
				    required: true
				    description: Database host
				""");
		artifact(root, "overlay", """
				imports:
				  - name: DB_HOST
				    required: true
				    description: Database host
				  - name: DB_PASSWORD
				    required: true
				    confidential: true
				""");

		var declarationsMaybe = ConfigurationImportDeclarations.read(new ClasspathIndex(root));

		assertThat(declarationsMaybe.isSatisfied()).isTrue();
		ConfigurationImportDeclarations declarations = declarationsMaybe.get();
		assertThat(declarations.names()).containsExactlyInAnyOrder("DB_HOST", "DB_PASSWORD");

		ConfigurationAssemblyReport report = declarations.report(List.of("DB_PASSWORD", "MISSING", "reflex.app.dir"));
		assertThat(report.getUnresolvedVariables()).containsExactly("DB_PASSWORD", "MISSING", "reflex.app.dir");
		assertThat(report.getUndeclaredVariables()).containsExactly("MISSING");
		assertThat(report.getPlatformVariables()).containsExactly("reflex.app.dir");
	}

	@Test
	public void rejectsIncompatibleDeclarations() throws Exception {
		Path root = temporaryFolder.newFolder("conflict").toPath();
		artifact(root, "base", """
				imports:
				  - name: DB_HOST
				    required: true
				""");
		artifact(root, "overlay", """
				imports:
				  - name: DB_HOST
				    required: false
				""");

		assertThat(ConfigurationImportDeclarations.read(new ClasspathIndex(root)).isUnsatisfied()).isTrue();
	}

	@Test
	public void resolvesClosedPropertiesAndTracesSymbolicAliasesToExternalLeaves() {
		var propertiesMaybe = SymbolicConfigurationProperties.analyze(Map.of(
				"DB_DEFAULT_PORT", "5432",
				"DB_DEFAULT_NAME", "proventem",
				"DB_DEFAULT_URL", "jdbc:postgresql://${DB_DEFAULT_HOST}:${DB_DEFAULT_PORT}/${DB_DEFAULT_NAME}",
				"HTTP_PORT", "8080"));

		assertThat(propertiesMaybe.isSatisfied()).isTrue();
		SymbolicConfigurationProperties properties = propertiesMaybe.get();
		assertThat(properties.resolveKnown("HTTP_PORT").get()).isEqualTo("8080");
		assertThat(properties.resolveKnown("DB_DEFAULT_URL").isUnsatisfied()).isTrue();
		assertThat(properties.externalLeaves(Set.of("DB_DEFAULT_URL"))).containsExactly("DB_DEFAULT_HOST");
	}

	@Test
	public void rejectsPropertyCycles() {
		var propertiesMaybe = SymbolicConfigurationProperties.analyze(Map.of(
				"A", "${B}",
				"B", "${A}"));

		assertThat(propertiesMaybe.isUnsatisfied()).isTrue();
	}

	@Test
	public void tracesDecryptFunctionsWithoutEvaluatingSecretsAtBuildTime() {
		var propertiesMaybe = SymbolicConfigurationProperties.analyze(Map.of(
				"RX_DECRYPT_SECRET", "${TRIBEFIRE_DECRYPT_SECRET}",
				"ENCRYPTED_PASSWORD", "${PASSWORD_CIPHER}",
				"LITERAL_PASSWORD", "${decrypt('cipher-text')}",
				"INDIRECT_PASSWORD", "${decrypt(${ENCRYPTED_PASSWORD})}"));

		assertThat(propertiesMaybe.isSatisfied()).isTrue();
		SymbolicConfigurationProperties properties = propertiesMaybe.get();
		assertThat(properties.resolveKnown("LITERAL_PASSWORD").isUnsatisfied()).isTrue();
		assertThat(properties.externalLeaves(Set.of("LITERAL_PASSWORD")))
				.containsExactly("TRIBEFIRE_DECRYPT_SECRET");
		assertThat(properties.externalLeaves(Set.of("INDIRECT_PASSWORD")))
				.containsExactlyInAnyOrder("TRIBEFIRE_DECRYPT_SECRET", "PASSWORD_CIPHER");
	}

	@Test
	public void rejectsUnsupportedPropertyFunctions() {
		var propertiesMaybe = SymbolicConfigurationProperties.analyze(Map.of(
				"VALUE", "${unknown('parameter')}"));

		assertThat(propertiesMaybe.isUnsatisfied()).isTrue();
		assertThat(propertiesMaybe.whyUnsatisfied().stringify()).contains("Unsupported configuration property operation: unknown");
	}

	@Test
	public void assemblesModeledLayersAndValidatesExternalLeaves() throws Exception {
		Path root = temporaryFolder.newFolder("assembly").toPath();
		artifact(root, "base", Map.of(
				"HICONIC-CONF/properties.yaml", """
						DB_DEFAULT_PORT: "5432"
						DB_DEFAULT_NAME: "proventem"
						DB_DEFAULT_URL: "jdbc:postgresql://${DB_DEFAULT_HOST}:${DB_DEFAULT_PORT}/${DB_DEFAULT_NAME}"
						""",
				"HICONIC-CONF/sample-configuration.yaml", """
						endpoint: "${DB_DEFAULT_URL}"
						label: base
						""",
				"HICONIC-CONF/unknown-configuration.yaml", """
						value: retained
						""",
				ConfigurationImportDeclarations.RESOURCE_PATH, """
						imports:
						  - name: DB_DEFAULT_HOST
						    required: true
						"""));
		artifact(root, "overlay", Map.of(
				"HICONIC-CONF/sample-configuration.overlay-10.yaml", """
						label: overlay
						"""));

		Path conf = temporaryFolder.newFolder("conf").toPath();
		Files.writeString(conf.resolve("sample-configuration.local-20.yaml"), "label: filesystem\n", StandardCharsets.UTF_8);

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				conf.toFile(),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isSatisfied()).isTrue();
		ConfigurationAssembly assembly = assemblyMaybe.get();
		assertThat(assembly.keys()).hasSize(1);
		SampleConfiguration configuration = (SampleConfiguration) assembly.configurations().values().iterator().next();
		assertThat(configuration.getLabel()).isEqualTo("filesystem");
		assertThat(assembly.report().getUnresolvedVariables()).containsExactly("DB_DEFAULT_HOST");
		assertThat(assembly.report().getUndeclaredVariables()).isEmpty();
		assertThat(assembly.report().getResidualResources()).containsExactly("unknown-configuration.yaml");
		assertThat(assembly.report().getAssembledConfigurations())
				.containsExactly(SampleConfiguration.T.getTypeSignature());

		Path output = temporaryFolder.newFolder("packaged-conf").toPath();
		Path rawProjection = output.resolve("sample-artifact/sample-configuration.yaml");
		Files.createDirectories(rawProjection.getParent());
		Files.writeString(rawProjection, "raw: retained\n");
		Path staleEffective = output.resolve("effective/stale-configuration.yaml");
		Files.createDirectories(staleEffective.getParent());
		Files.writeString(staleEffective, "stale: true\n");

		var writeMaybe = ConfigurationAssemblyWriter.write(assembly, output);
		assertThat(writeMaybe.isSatisfied()).isTrue();
		String effectiveYaml = Files.readString(output.resolve("effective/sample-configuration.yaml"));
		assertThat(effectiveYaml).contains("label: \"filesystem\"");
		assertThat(effectiveYaml).contains("${DB_DEFAULT_URL}");
		assertThat(staleEffective).doesNotExist();
		assertThat(rawProjection).exists();
		assertThat(output.resolve(ConfigurationAssemblyWriter.REPORT_FILE)).exists();
	}

	@Test
	public void rejectsAnExternalLeafWhichNoArtifactDeclared() throws Exception {
		Path root = temporaryFolder.newFolder("undeclared").toPath();
		artifact(root, "base", Map.of(
				"HICONIC-CONF/properties.yaml", """
						DB_DEFAULT_URL: "jdbc:postgresql://${DB_DEFAULT_HOST}/proventem"
						""",
				"HICONIC-CONF/sample-configuration.yaml", """
						endpoint: "${DB_DEFAULT_URL}"
						"""));

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				temporaryFolder.newFolder("empty-conf"),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isUnsatisfied()).isTrue();
		assertThat(assemblyMaybe.value().report().getUndeclaredVariables()).containsExactly("DB_DEFAULT_HOST");
	}

	@Test
	public void preservesUseCaseAsPartOfTheCanonicalConfigurationKey() throws Exception {
		Path root = temporaryFolder.newFolder("use-case").toPath();
		artifact(root, "worker", Map.of(
				"HICONIC-CONF/sample-configuration~worker.yaml", """
						label: worker
						"""));

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				temporaryFolder.newFolder("use-case-conf"),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isSatisfied()).isTrue();
		ConfigurationKey key = assemblyMaybe.get().keys().iterator().next();
		assertThat(key.useCase()).isEqualTo("worker");
		assertThat(key.fileName()).isEqualTo("sample-configuration~worker.yaml");
		assertThat(((SampleConfiguration) assemblyMaybe.get().configurations().get(key)).getLabel()).isEqualTo("worker");
	}

	@Test
	public void rejectsAmbiguousAndMalformedModeledResourceNames() throws Exception {
		Path root = temporaryFolder.newFolder("invalid-discovery").toPath();
		artifact(root, "invalid", Map.of(
				"HICONIC-CONF/sample-configuration.yaml", "label: value\n",
				"HICONIC-CONF/sample-configuration~.yaml", "label: value\n"));

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				temporaryFolder.newFolder("invalid-discovery-conf"),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T,
						hiconic.rx.platform.configuration.model.alternative.SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isUnsatisfied()).isTrue();
		String reason = assemblyMaybe.whyUnsatisfied().stringify();
		assertThat(reason).contains("Ambiguous modeled configuration resource");
		assertThat(reason).contains("Malformed modeled configuration resource name");
	}

	@Test
	public void reportsInvalidPropertyGraphsThroughTheAssemblyBoundary() throws Exception {
		Path root = temporaryFolder.newFolder("cyclic-assembly").toPath();
		artifact(root, "cycle", Map.of(
				"HICONIC-CONF/properties.yaml", """
						A: "${B}"
						B: "${A}"
						""",
				"HICONIC-CONF/sample-configuration.yaml", """
						endpoint: "${A}"
						"""));

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				temporaryFolder.newFolder("cyclic-assembly-conf"),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isUnsatisfied()).isTrue();
		assertThat(assemblyMaybe.whyUnsatisfied().stringify()).contains("Cyclic configuration property reference");
	}

	@Test
	public void reportsConflictingImportsThroughTheAssemblyBoundary() throws Exception {
		Path root = temporaryFolder.newFolder("conflicting-import-assembly").toPath();
		artifact(root, "first", """
				imports:
				  - name: DB_HOST
				    required: true
				""");
		artifact(root, "second", """
				imports:
				  - name: DB_HOST
				    required: false
				""");

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				temporaryFolder.newFolder("conflicting-import-assembly-conf"),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isUnsatisfied()).isTrue();
		assertThat(assemblyMaybe.whyUnsatisfied().stringify()).contains("Incompatible declarations for configuration import");
	}

	private static void artifact(Path root, String artifact, String declarations) throws Exception {
		artifact(root, artifact, Map.of(ConfigurationImportDeclarations.RESOURCE_PATH, declarations));
	}

	private static void artifact(Path root, String artifact, Map<String, String> resources) throws Exception {
		Path artifactRoot = root.resolve(artifact);
		Path metaInf = artifactRoot.resolve("META-INF");
		Files.createDirectories(metaInf);
		Files.writeString(metaInf.resolve("classpath-index.txt"),
				String.join("\n", resources.keySet().stream().sorted().toList()) + "\n",
				StandardCharsets.UTF_8);
		Files.writeString(metaInf.resolve("classpath-origin.properties"), "artifactId=" + artifact + "\n", StandardCharsets.UTF_8);
		for (Map.Entry<String, String> resource : resources.entrySet()) {
			Path file = artifactRoot.resolve(resource.getKey());
			Files.createDirectories(file.getParent());
			Files.writeString(file, resource.getValue(), StandardCharsets.UTF_8);
		}
	}
}
