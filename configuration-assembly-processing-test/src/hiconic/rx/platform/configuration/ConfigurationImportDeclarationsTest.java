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

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.gm.config.assembly.model.ConfigurationAssemblyReport;
import com.braintribe.gm.config.assembly.model.PackagedResourceIndex;
import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.model.bvd.resource.PackagedResource;
import com.braintribe.model.bvd.resource.PackagedResourceText;
import com.braintribe.model.bvd.resource.PackagedSource;
import com.braintribe.model.bvd.resource.ResourceText;

import hiconic.rx.platform.model.configuration.vd.Decrypt;

import hiconic.rx.platform.conf.RxPropertyResolver;
import hiconic.rx.platform.configuration.model.SampleConfiguration;
import hiconic.rx.platform.loading.RxPropertiesLoader;
import hiconic.rx.platform.processing.resource.RxIndexedPackagedResourceResolver;

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
		assertThat(declarations.origins("DB_HOST")).containsExactly("base", "overlay");
		assertThat(declarations.origins("DB_PASSWORD")).containsExactly("overlay");

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
				"HICONIC-CONF/sample-configuration/logo.svg", "<svg>retained beside its modeled configuration</svg>",
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

		assertThat(assemblyMaybe.isSatisfied())
				.withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify())
				.isTrue();
		ConfigurationAssembly assembly = assemblyMaybe.get();
		assertThat(assembly.keys()).hasSize(1);
		SampleConfiguration configuration = (SampleConfiguration) assembly.configurations().values().iterator().next();
		assertThat(configuration.getLabel()).isEqualTo("filesystem");
		assertThat(assembly.report().getUnresolvedVariables()).containsExactly("DB_DEFAULT_HOST");
		assertThat(assembly.report().getUndeclaredVariables()).isEmpty();
		assertThat(assembly.report().getResidualResources()).containsExactly("unknown-configuration.yaml");
		assertThat(assembly.report().getAssembledConfigurations())
				.containsExactly(SampleConfiguration.T.getTypeSignature());

		Path output = temporaryFolder.newFolder("effective-conf").toPath();
		Path staleEffective = output.resolve("compiled/stale-configuration.yaml");
		Files.createDirectories(staleEffective.getParent());
		Files.writeString(staleEffective, "stale: true\n");
		Path protocol = output.getParent().resolve("configuration-compilation.yaml");

		var writeMaybe = ConfigurationAssemblyWriter.write(assembly, new ClasspathIndex(root), output, protocol);
		assertThat(writeMaybe.isSatisfied()).isTrue();
		String effectiveYaml = Files.readString(output.resolve("compiled/sample-configuration.yaml"));
		assertThat(effectiveYaml).contains("label: \"filesystem\"");
		assertThat(effectiveYaml).contains("${DB_DEFAULT_URL}");
		assertThat(staleEffective).doesNotExist();
		assertThat(output.resolve("base/unknown-configuration.yaml")).hasContent("value: retained");
		assertThat(output.resolve("base/sample-configuration/logo.svg"))
				.hasContent("<svg>retained beside its modeled configuration</svg>");
		assertThat(assembly.report().getResidualResources())
				.containsExactlyInAnyOrder("base/sample-configuration/logo.svg", "base/unknown-configuration.yaml");

		ClasspathIndex runtimeIndex = new ClasspathIndex(List.of(
				ClasspathIndex.filesystemSource(root, "", List.of("HICONIC-CONF/")),
				ClasspathIndex.filesystemSlots(output, "HICONIC-CONF")));
		assertThat(runtimeIndex.forPrefix("HICONIC-CONF/sample-configuration/logo.svg"))
				.extracting(entry -> entry.artifactId)
				.containsExactly("base");
		Path compiledProperties = output.resolve("compiled/properties.yaml");
		assertThat(compiledProperties).content()
				.contains("DB_DEFAULT_URL")
				.contains("jdbc:postgresql://${DB_DEFAULT_HOST}:${DB_DEFAULT_PORT}/${DB_DEFAULT_NAME}")
				.doesNotContain("$${DB_DEFAULT_HOST}");

		var runtimePropertiesMaybe = RxPropertiesLoader.load(compiledProperties.toFile(), new YamlMarshaller());
		assertThat(runtimePropertiesMaybe.isSatisfied()).isTrue();
		Map<String, String> runtimeProperties = new LinkedHashMap<>(runtimePropertiesMaybe.get());
		runtimeProperties.put("DB_DEFAULT_HOST", "database.internal");

		RxPropertyResolver runtimeResolver = new RxPropertyResolver();
		runtimeResolver.setManagedPropertiesOnly(true);
		runtimeResolver.setRawProperties(runtimeProperties);
		assertThat(runtimeResolver.resolve("DB_DEFAULT_URL"))
				.isEqualTo("jdbc:postgresql://database.internal:5432/proventem");
		assertThat(protocol).exists();
	}

	@Test
	public void preservesPackagedResourceExpressionsForRuntimeWithoutTreatingThemAsImports() throws Exception {
		Path root = temporaryFolder.newFolder("packaged-resources").toPath();
		artifact(root, "resources", Map.of(
				"HICONIC-CONF/sample-configuration.yaml", """
						endpoint: "${packagedResourceText('./endpoint.txt')}"
						""",
				"HICONIC-CONF/endpoint.txt", "https://example.org/service"));

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				temporaryFolder.newFolder("empty-conf"),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isSatisfied()).isTrue();
		SampleConfiguration configuration = (SampleConfiguration) assemblyMaybe.get().configurations().values().iterator().next();
		PackagedResourceText resourceText = SampleConfiguration.T.getProperty("endpoint").getVdDirect(configuration);
		assertThat(resourceText).isNotNull();
		assertThat(resourceText.getPath()).isEqualTo("./endpoint.txt");

		Path output = temporaryFolder.newFolder("packaged-resources-effective").toPath();
		Path protocol = output.getParent().resolve("packaged-resources-compilation.yaml");
		var writeMaybe = ConfigurationAssemblyWriter.write(assemblyMaybe.get(), new ClasspathIndex(root), output, protocol);
		assertThat(writeMaybe.isSatisfied())
				.withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify())
				.isTrue();
		assertThat(Files.readString(output.resolve("compiled/sample-configuration.yaml")))
				.contains("${packagedResourceText('./endpoint.txt', 'resources', 'UTF-8')}")
				.doesNotContain("https://example.org/service");
		assertThat(assemblyMaybe.get().report().getUndeclaredVariables()).isEmpty();
	}

	@Test
	public void preservesEveryPackagedResourceDescriptorForRuntime() throws Exception {
		Path root = temporaryFolder.newFolder("all-packaged-resource-descriptors").toPath();
		artifact(root, "resources", Map.of(
				"HICONIC-CONF/sample-configuration.yaml", """
						endpoint: "${resourceText(packagedResource('./endpoint.txt'))}"
						resource: "${packagedResource('./endpoint.txt')}"
						source: "${packagedSource('./endpoint.txt')}"
						""",
				"HICONIC-CONF/endpoint.txt", "https://example.org/service"));

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				temporaryFolder.newFolder("empty-conf"),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isSatisfied())
				.withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify())
				.isTrue();
		SampleConfiguration configuration = (SampleConfiguration) assemblyMaybe.get().configurations().values().iterator().next();
		ResourceText resourceText = SampleConfiguration.T.getProperty("endpoint").getVdDirect(configuration);
		assertThat(resourceText).isNotNull();
		assertThat((Object) ResourceText.T.getProperty("resource").getVdDirect(resourceText)).isInstanceOf(PackagedResource.class);
		assertThat((Object) SampleConfiguration.T.getProperty("resource").getVdDirect(configuration)).isInstanceOf(PackagedResource.class);
		assertThat((Object) SampleConfiguration.T.getProperty("source").getVdDirect(configuration)).isInstanceOf(PackagedSource.class);
		assertThat(assemblyMaybe.get().report().getUndeclaredVariables()).isEmpty();
	}

	@Test
	public void preservesEncryptedPackagedResourceExpressionsWithoutTreatingThemAsImports() throws Exception {
		Path root = temporaryFolder.newFolder("encrypted-packaged-resources").toPath();
		artifact(root, "resources", Map.of(
				"HICONIC-CONF/properties.yaml", "RX_DECRYPT_SECRET: \"${DEPLOYMENT_DECRYPT_SECRET}\"\n",
				"HICONIC-CONF/sample-configuration.yaml", """
						endpoint: "${decrypt(packagedResourceText('./endpoint.encrypted'))}"
						""",
				"HICONIC-CONF/endpoint.encrypted", "encrypted-value",
				ConfigurationImportDeclarations.RESOURCE_PATH, """
						imports:
						  - name: DEPLOYMENT_DECRYPT_SECRET
						    required: true
						    confidential: true
						"""));

		var assemblyMaybe = new ModeledConfigurationAssembler(
				new ClasspathIndex(root),
				temporaryFolder.newFolder("empty-conf"),
				"HICONIC-CONF",
				List.of(SampleConfiguration.T))
				.assemble();

		assertThat(assemblyMaybe.isSatisfied())
				.withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify())
				.isTrue();
		SampleConfiguration configuration = (SampleConfiguration) assemblyMaybe.get().configurations().values().iterator().next();
		Decrypt decrypt = SampleConfiguration.T.getProperty("endpoint").getVdDirect(configuration);
		assertThat((Object) Decrypt.T.getProperty("value").getVdDirect(decrypt)).isInstanceOf(PackagedResourceText.class);

		Path output = temporaryFolder.newFolder("encrypted-packaged-resources-effective").toPath();
		Path protocol = output.getParent().resolve("encrypted-packaged-resources-compilation.yaml");
		var writeMaybe = ConfigurationAssemblyWriter.write(assemblyMaybe.get(), new ClasspathIndex(root), output, protocol);
		assertThat(writeMaybe.isSatisfied())
				.withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify())
				.isTrue();
		String yaml = Files.readString(output.resolve("compiled/sample-configuration.yaml"));
		assertThat(yaml)
				.contains("${decrypt(packagedResourceText('./endpoint.encrypted', 'resources', 'UTF-8'))}")
				.doesNotContain("encrypted-value");
		assertThat(assemblyMaybe.get().report().getUndeclaredVariables()).isEmpty();
	}

	@Test
	public void integralAssemblyKeepsDeferredExpressionAndMakesItsContentReadable() throws Exception {
		Path root = temporaryFolder.newFolder("integral-resource-mirror").toPath();
		artifact(root, "resources", Map.of(
				"HICONIC-CONF/properties.yaml", "RX_DECRYPT_SECRET: \"${DEPLOYMENT_DECRYPT_SECRET}\"\n",
				"HICONIC-CONF/sample-configuration.yaml", "endpoint: \"${decrypt(packagedResourceText('./endpoint.encrypted'))}\"\n",
				"HICONIC-CONF/endpoint.encrypted", "cipher payload",
				ConfigurationImportDeclarations.RESOURCE_PATH, """
						imports:
						  - name: DEPLOYMENT_DECRYPT_SECRET
						    required: true
						    confidential: true
						"""));
		ClasspathIndex sourceIndex = new ClasspathIndex(root);
		var assemblyMaybe = new ModeledConfigurationAssembler(sourceIndex, temporaryFolder.newFolder("integral-empty-conf"),
				"HICONIC-CONF", List.of(SampleConfiguration.T)).assemble();
		assertThat(assemblyMaybe.isSatisfied()).withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify()).isTrue();

		Path app = temporaryFolder.newFolder("integral-app").toPath();
		Path conf = app.resolve("conf");
		Path resourceIndexFile = app.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE);
		var writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), sourceIndex, conf, resourceIndexFile,
				app.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE), Map.of("resources", "test:resources"));
		assertThat(writeMaybe.isSatisfied()).withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify()).isTrue();

		String yaml = Files.readString(conf.resolve("sample-configuration.yaml"));
		assertThat(yaml).contains("${decrypt(packagedResourceText('./endpoint.encrypted', null, 'UTF-8'))}")
				.doesNotContain("cipher payload");
		assertThat(Files.readString(conf.resolve("endpoint.encrypted"))).isEqualTo("cipher payload");

		PackagedResourceIndex index = YamlConfigurations.read(PackagedResourceIndex.T).from(resourceIndexFile.toFile()).get();
		var runtimeResolver = new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);
		try (var in = runtimeResolver.openStream("compiled", "HICONIC-CONF/endpoint.encrypted").get()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("cipher payload");
		}
	}

	@Test
	public void integralAssemblyRenamesOnlyCollidingResourcesAndKeepsBothAddressable() throws Exception {
		Path root = temporaryFolder.newFolder("colliding-resource-mirror").toPath();
		artifact(root, "artifact-a", Map.of(
				"HICONIC-CONF/sample-configuration.a-10.yaml", "headers:\n  A: \"${packagedResourceText('./shared.txt')}\"\n",
				"HICONIC-CONF/shared.txt", "from a"));
		artifact(root, "artifact-b", Map.of(
				"HICONIC-CONF/sample-configuration.b-20.yaml", "headers:\n  B: \"${packagedResourceText('./shared.txt')}\"\n",
				"HICONIC-CONF/shared.txt", "from b"));
		ClasspathIndex sourceIndex = new ClasspathIndex(root);
		var assemblyMaybe = new ModeledConfigurationAssembler(sourceIndex, temporaryFolder.newFolder("collision-empty-conf"),
				"HICONIC-CONF", List.of(SampleConfiguration.T)).assemble();
		assertThat(assemblyMaybe.isSatisfied()).withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify()).isTrue();

		Path app = temporaryFolder.newFolder("collision-app").toPath();
		Path conf = app.resolve("conf");
		Path indexFile = app.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE);
		var writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), sourceIndex, conf, indexFile,
				app.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE),
				Map.of("artifact-a", "test:artifact-a", "artifact-b", "test:artifact-b"));
		assertThat(writeMaybe.isSatisfied()).withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify()).isTrue();

		PackagedResourceIndex index = YamlConfigurations.read(PackagedResourceIndex.T).from(indexFile.toFile()).get();
		assertThat(index.getArtifacts()).containsOnlyKeys("test:artifact-a", "test:artifact-b");
		assertThat(index.getArtifacts().values().stream()
				.flatMap(section -> section.getResources().stream()).filter(resource -> resource.getMaterializedAs() != null).count())
				.isEqualTo(1);
		String yaml = Files.readString(conf.resolve("sample-configuration.yaml"));
		assertThat(yaml).contains("test:artifact-a").contains("test:artifact-b");

		var resolver = new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);
		try (var a = resolver.openStream("test:artifact-a", "HICONIC-CONF/shared.txt").get();
				var b = resolver.openStream("test:artifact-b", "HICONIC-CONF/shared.txt").get()) {
			assertThat(new String(a.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("from a");
			assertThat(new String(b.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("from b");
		}
	}

	@Test
	public void integralAssemblyCompilesLogLevelsAndRetainsOtherUnmodeledResourcesWithArtifactIdentity() throws Exception {
		Path root = temporaryFolder.newFolder("residual-resource-mirror").toPath();
		artifact(root, "artifact-a", Map.of(
				"HICONIC-CONF/sample-configuration.yaml", "label: assembled\n",
				"HICONIC-CONF/log-levels.properties", "a=INFO\n"));
		artifact(root, "artifact-b", Map.of(
				"HICONIC-CONF/log-levels.properties", "b=DEBUG\n",
				"HICONIC-CONF/webapp-dependencies.properties", "ui=test:ui#1.0\n"));
		ClasspathIndex sourceIndex = new ClasspathIndex(root);
		var assemblyMaybe = new ModeledConfigurationAssembler(sourceIndex, temporaryFolder.newFolder("residual-empty-conf"),
				"HICONIC-CONF", List.of(SampleConfiguration.T)).assemble();
		assertThat(assemblyMaybe.isSatisfied()).withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify()).isTrue();

		Path app = temporaryFolder.newFolder("residual-integral-app").toPath();
		Path conf = app.resolve("conf");
		Files.createDirectories(conf);
		Files.writeString(conf.resolve("log-levels.pipeline-50.properties"), "c=WARN\n");
		Path indexFile = app.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE);
		var writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), sourceIndex, conf, indexFile,
				app.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE),
				Map.of("artifact-a", "test:artifact-a", "artifact-b", "test:artifact-b"));
		assertThat(writeMaybe.isSatisfied()).withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify()).isTrue();

		PackagedResourceIndex index = YamlConfigurations.read(PackagedResourceIndex.T).from(indexFile.toFile()).get();
		assertThat(index.getArtifacts()).containsOnlyKeys("test:artifact-b");
		assertThat(index.getArtifacts().values().stream().flatMap(section -> section.getResources().stream())
				.map(resource -> resource.getPath()).toList())
				.containsExactly("HICONIC-CONF/webapp-dependencies.properties");
		assertThat(Files.list(conf).filter(path -> path.getFileName().toString().startsWith("log-levels"))).hasSize(1);
		assertThat(conf.resolve("log-levels.properties")).hasContent("a=INFO\nb=DEBUG\nc=WARN\n");
		assertThat(conf.resolve("webapp-dependencies.properties")).hasContent("ui=test:ui#1.0");
		assertThat(assemblyMaybe.get().report().getResidualResources())
				.contains("webapp-dependencies.properties")
				.noneMatch(path -> path.startsWith("log-levels"));
		assertThat(Files.readString(app.resolve(ConfigurationAssemblyWriter.MERGE_PROTOCOL_FILE)))
				.contains("\"conf/log-levels.properties\"")
				.contains("artifact: \"test:artifact-a\"")
				.contains("artifact: \"test:artifact-b\"")
				.contains("artifact: \"pipeline\"")
				.contains("resource: \"conf/log-levels.pipeline-50.properties\"");
	}

	@Test
	public void integralAssemblyMaterializesAllPackagedResourcesAndResolvesNonConfigurationCollisions() throws Exception {
		Path root = temporaryFolder.newFolder("complete-resource-mirror").toPath();
		artifact(root, "artifact-a", Map.of(
				"HICONIC-CONF/sample-configuration.yaml", "label: assembled\n",
				"assets/shared.txt", "asset from a",
				"assets/only-a.txt", "only a"));
		artifact(root, "artifact-b", Map.of(
				"assets/shared.txt", "asset from b",
				"fonts/font.bin", "font payload"));
		ClasspathIndex sourceIndex = new ClasspathIndex(root);
		var assemblyMaybe = new ModeledConfigurationAssembler(sourceIndex, temporaryFolder.newFolder("complete-empty-conf"),
				"HICONIC-CONF", List.of(SampleConfiguration.T)).assemble();
		assertThat(assemblyMaybe.isSatisfied()).withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify()).isTrue();

		Path app = temporaryFolder.newFolder("complete-integral-app").toPath();
		Path conf = app.resolve("conf");
		Path indexFile = app.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE);
		var writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), sourceIndex, conf, indexFile,
				app.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE),
				Map.of("artifact-a", "test:artifact-a", "artifact-b", "test:artifact-b"));
		assertThat(writeMaybe.isSatisfied()).withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify()).isTrue();

		assertThat(conf.resolve("assets/only-a.txt")).hasContent("only a");
		assertThat(conf.resolve("fonts/font.bin")).hasContent("font payload");
		assertThat(Files.list(conf.resolve("assets")).filter(path -> path.getFileName().toString().startsWith("shared"))).hasSize(2);

		PackagedResourceIndex index = YamlConfigurations.read(PackagedResourceIndex.T).from(indexFile.toFile()).get();
		assertThat(index.getArtifacts()).containsOnlyKeys("test:artifact-a", "test:artifact-b");
		assertThat(index.getArtifacts().values().stream().flatMap(section -> section.getResources().stream())
				.map(resource -> resource.getPath()).toList())
				.containsExactlyInAnyOrder("assets/shared.txt", "assets/only-a.txt", "assets/shared.txt", "fonts/font.bin");
		assertThat(index.getArtifacts().values().stream().flatMap(section -> section.getResources().stream())
				.filter(resource -> resource.getPath().equals("assets/shared.txt"))
				.filter(resource -> resource.getMaterializedAs() != null).count()).isEqualTo(1);

		var resolver = new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);
		try (var a = resolver.openStream("test:artifact-a", "assets/shared.txt").get();
				var b = resolver.openStream("test:artifact-b", "assets/shared.txt").get();
				var font = resolver.openStream("test:artifact-b", "fonts/font.bin").get()) {
			assertThat(new String(a.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("asset from a");
			assertThat(new String(b.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("asset from b");
			assertThat(new String(font.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("font payload");
		}
	}

	@Test
	public void integralAssemblyDoesNotRematerializePremergedJvmOptions() throws Exception {
		Path root = temporaryFolder.newFolder("premerged-jvm-resource-mirror").toPath();
		artifact(root, "artifact-a", Map.of(
				"HICONIC-CONF/sample-configuration.yaml", "label: assembled\n",
				"HICONIC-CONF/jvm.options", "-Done=a\n",
				"assets/retained.txt", "retained"));
		artifact(root, "artifact-b", Map.of(
				"HICONIC-CONF/jvm.options", "-Dtwo=b\n"));
		ClasspathIndex sourceIndex = new ClasspathIndex(root);
		var assemblyMaybe = new ModeledConfigurationAssembler(sourceIndex, temporaryFolder.newFolder("premerged-jvm-empty-conf"),
				"HICONIC-CONF", List.of(SampleConfiguration.T)).assemble();
		assertThat(assemblyMaybe.isSatisfied()).withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify()).isTrue();

		Path app = temporaryFolder.newFolder("premerged-jvm-integral-app").toPath();
		Path conf = app.resolve("conf");
		Files.createDirectories(conf);
		Files.writeString(conf.resolve("jvm.options"), "-Done=a\n-Dtwo=b\n");
		Path indexFile = app.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE);
		var writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), sourceIndex, conf, indexFile,
				app.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE),
				Map.of("artifact-a", "test:artifact-a", "artifact-b", "test:artifact-b"));
		assertThat(writeMaybe.isSatisfied()).withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify()).isTrue();

		assertThat(conf.resolve("jvm.options")).hasContent("-Done=a\n-Dtwo=b\n");
		assertThat(Files.list(conf).filter(path -> path.getFileName().toString().startsWith("jvm--"))).isEmpty();
		assertThat(conf.resolve("assets/retained.txt")).hasContent("retained");

		PackagedResourceIndex index = YamlConfigurations.read(PackagedResourceIndex.T).from(indexFile.toFile()).get();
		assertThat(index.getArtifacts().values().stream().flatMap(section -> section.getResources().stream())
				.map(resource -> resource.getPath()).toList())
				.contains("assets/retained.txt")
				.doesNotContain("HICONIC-CONF/jvm.options");
		assertThat(assemblyMaybe.get().report().getResidualResources())
				.contains("assets/retained.txt")
				.noneMatch(path -> path.startsWith("jvm--"));

		String mergeProtocol = Files.readString(app.resolve(ConfigurationAssemblyWriter.MERGE_PROTOCOL_FILE));
		assertThat(mergeProtocol)
				.contains("\"conf/jvm.options\"")
				.contains("operation: \"merged\"")
				.contains("artifact: \"test:artifact-a\"")
				.contains("artifact: \"test:artifact-b\"")
				.contains("resource: \"HICONIC-CONF/jvm.options\"")
				.contains("\"conf/sample-configuration.yaml\"")
				.contains("operation: \"compiled\"");
	}

	@Test
	public void preservesDecryptDescriptorsInPropertiesAndCollectionsAcrossAssemblyRoundtrip() throws Exception {
		Path root = temporaryFolder.newFolder("residual-decrypt").toPath();
		artifact(root, "encrypted", Map.of(
				"HICONIC-CONF/properties.yaml", "RX_DECRYPT_SECRET: \"${DEPLOYMENT_DECRYPT_SECRET}\"\n",
				"HICONIC-CONF/sample-configuration.yaml", """
						endpoint: "${decrypt('endpoint-cipher')}"
						headers:
						  Client-Id: "${decrypt('client-id-cipher')}"
						""",
				ConfigurationImportDeclarations.RESOURCE_PATH, """
						imports:
						  - name: DEPLOYMENT_DECRYPT_SECRET
						    required: true
						    confidential: true
						"""));

		ClasspathIndex index = new ClasspathIndex(root);
		var assemblyMaybe = new ModeledConfigurationAssembler(index,
				temporaryFolder.newFolder("residual-decrypt-conf"), "HICONIC-CONF", List.of(SampleConfiguration.T)).assemble();

		assertThat(assemblyMaybe.isSatisfied())
				.withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify())
				.isTrue();
		SampleConfiguration configuration = (SampleConfiguration) assemblyMaybe.get().configurations().values().iterator().next();
		assertThat((Object) SampleConfiguration.T.getProperty("endpoint").getVdDirect(configuration)).isInstanceOf(Decrypt.class);
		Object headerValue = configuration.getHeaders().get("Client-Id");
		assertThat(headerValue).isInstanceOf(Decrypt.class);

		Path output = temporaryFolder.newFolder("residual-decrypt-effective").toPath();
		Path protocol = output.getParent().resolve("residual-decrypt-compilation.yaml");
		var writeMaybe = ConfigurationAssemblyWriter.write(assemblyMaybe.get(), index, output, protocol);
		assertThat(writeMaybe.isSatisfied())
				.withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify())
				.isTrue();

		String yaml = Files.readString(output.resolve("compiled/sample-configuration.yaml"));
		assertThat(yaml)
				.contains("${decrypt('endpoint-cipher')}")
				.contains("${decrypt('client-id-cipher')}")
				.doesNotContain("VD (Decrypt-gm)")
				.doesNotContain("endpoint: \"\"");
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
	public void integralAssemblyResolvesArtifactRootPathsAndReadsTheirContent() throws Exception {
		Path root = temporaryFolder.newFolder("absolute-resource-mirror").toPath();
		artifact(root, "absolute", Map.of(
				"HICONIC-CONF/sample-configuration.yaml", "endpoint: \"${packagedResourceText('/artifact-root.txt')}\"\n",
				"artifact-root.txt", "artifact root payload"));
		ClasspathIndex sourceIndex = new ClasspathIndex(root);
		var assemblyMaybe = new ModeledConfigurationAssembler(sourceIndex,
				temporaryFolder.newFolder("absolute-empty-conf"), "HICONIC-CONF", List.of(SampleConfiguration.T)).assemble();
		assertThat(assemblyMaybe.isSatisfied()).withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify()).isTrue();

		Path app = temporaryFolder.newFolder("absolute-app").toPath();
		Path conf = app.resolve("conf");
		Path indexFile = app.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE);
		var writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), sourceIndex, conf, indexFile,
				app.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE), Map.of("absolute", "test:absolute"));
		assertThat(writeMaybe.isSatisfied()).withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify()).isTrue();
		assertThat(Files.readString(conf.resolve("sample-configuration.yaml")))
				.contains("${packagedResourceText('/artifact-root.txt', null, 'UTF-8')}");
		assertThat(Files.readString(conf.resolve("artifact-root.txt"))).isEqualTo("artifact root payload");

		PackagedResourceIndex index = YamlConfigurations.read(PackagedResourceIndex.T).from(indexFile.toFile()).get();
		var resolver = new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);
		try (var in = resolver.openStream("compiled", "artifact-root.txt").get()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("artifact root payload");
		}
	}

	@Test
	public void integralAssemblyKeepsPipelineSiblingResourcesInPlace() throws Exception {
		Path emptyResources = temporaryFolder.newFolder("pipeline-empty-resources").toPath();
		ClasspathIndex sourceIndex = new ClasspathIndex(emptyResources);
		Path app = temporaryFolder.newFolder("pipeline-app").toPath();
		Path conf = app.resolve("conf");
		Files.createDirectories(conf);
		Files.writeString(conf.resolve("sample-configuration.yaml"),
				"endpoint: \"${packagedResourceText('./pipeline.pem')}\"\n", StandardCharsets.UTF_8);
		Files.writeString(conf.resolve("pipeline.pem"), "pipeline payload", StandardCharsets.UTF_8);

		var assemblyMaybe = new ModeledConfigurationAssembler(sourceIndex, conf.toFile(), "HICONIC-CONF",
				List.of(SampleConfiguration.T)).assemble();
		assertThat(assemblyMaybe.isSatisfied()).withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify()).isTrue();
		Path indexFile = app.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE);
		var writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), sourceIndex, conf, indexFile,
				app.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE), Map.of());
		assertThat(writeMaybe.isSatisfied()).withFailMessage(() -> writeMaybe.whyUnsatisfied().stringify()).isTrue();
		assertThat(Files.readString(conf.resolve("sample-configuration.yaml")))
				.contains("${packagedResourceText('./pipeline.pem', null, 'UTF-8')}");
		assertThat(Files.readString(conf.resolve("pipeline.pem"))).isEqualTo("pipeline payload");

		PackagedResourceIndex index = YamlConfigurations.read(PackagedResourceIndex.T).from(indexFile.toFile()).get();
		assertThat(index.getArtifacts()).containsOnlyKeys("pipeline");
		var resolver = new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);
		try (var in = resolver.openStream("compiled", "HICONIC-CONF/pipeline.pem").get()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("pipeline payload");
		}
	}

	@Test
	public void integralAssemblyRejectsAReferencedArtifactWithoutAFullCoordinate() throws Exception {
		Path root = temporaryFolder.newFolder("ambiguous-coordinate-resources").toPath();
		artifact(root, "annotations", Map.of(
				"HICONIC-CONF/sample-configuration.yaml", "endpoint: \"${packagedResourceText('./payload.txt')}\"\n",
				"HICONIC-CONF/payload.txt", "payload"));
		ClasspathIndex sourceIndex = new ClasspathIndex(root);
		var assemblyMaybe = new ModeledConfigurationAssembler(sourceIndex,
				temporaryFolder.newFolder("ambiguous-coordinate-conf"), "HICONIC-CONF", List.of(SampleConfiguration.T)).assemble();
		assertThat(assemblyMaybe.isSatisfied()).withFailMessage(() -> assemblyMaybe.whyUnsatisfied().stringify()).isTrue();

		Path app = temporaryFolder.newFolder("ambiguous-coordinate-app").toPath();
		var writeMaybe = ConfigurationAssemblyWriter.writeIntegrated(assemblyMaybe.get(), sourceIndex, app.resolve("conf"),
				app.resolve(ConfigurationAssemblyWriter.RESOURCE_INDEX_FILE), app.resolve(ConfigurationAssemblyWriter.PROTOCOL_FILE), Map.of());
		assertThat(writeMaybe.isUnsatisfied()).isTrue();
		assertThat(writeMaybe.whyUnsatisfied().stringify()).contains("Cannot determine the full coordinate")
				.contains("annotations");
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
