package hiconic.rx.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.braintribe.gm.config.yaml.index.ClasspathEntry;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.ve.api.VirtualEnvironment;

import hiconic.rx.platform.conf.ConfigurationPropertyImports;

public class ConfigurationPropertyImportsTest {

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final Map<String, String> environment = new java.util.LinkedHashMap<>();
	private final Map<String, String> systemProperties = new java.util.LinkedHashMap<>();

	@Test
	public void bindsOnlyDeclaredImportsAndPrefersSystemProperties() throws Exception {
		ConfigurationImportDeclarations declarations = declarations("""
				imports:
				  - name: REQUIRED
				    required: true
				  - name: OPTIONAL
				    required: false
				""");
		environment.put("REQUIRED", "environment");
		systemProperties.put("REQUIRED", "system");
		environment.put("UNDECLARED", "invisible");

		Maybe<Map<String, String>> result = ConfigurationPropertyImports.bind(Map.of("LOCAL", "local"), declarations, virtualEnvironment());

		assertThat(result.isSatisfied()).isTrue();
		assertThat(result.get()).containsEntry("LOCAL", "local").containsEntry("REQUIRED", "system");
		assertThat(result.get()).doesNotContainKeys("OPTIONAL", "UNDECLARED");
	}

	@Test
	public void locallyManagedValueSatisfiesRequiredImportWithoutHostLookup() throws Exception {
		ConfigurationImportDeclarations declarations = declarations("""
				imports:
				  - name: REQUIRED
				    required: true
				""");

		Maybe<Map<String, String>> result = ConfigurationPropertyImports.bind(Map.of("REQUIRED", "local"), declarations,
				virtualEnvironment());

		assertThat(result.isSatisfied()).isTrue();
		assertThat(result.get()).containsEntry("REQUIRED", "local");
	}

	@Test
	public void rejectsMissingRequiredImport() throws Exception {
		ConfigurationImportDeclarations declarations = declarations("""
				imports:
				  - name: REQUIRED
				    required: true
				""");

		Maybe<Map<String, String>> result = ConfigurationPropertyImports.bind(Map.of(), declarations, virtualEnvironment());

		assertThat(result.isUnsatisfied()).isTrue();
		assertThat(result.whyUnsatisfied().stringify()).contains("REQUIRED");
	}

	private ConfigurationImportDeclarations declarations(String yaml) throws Exception {
		File file = temporaryFolder.newFile("configuration-imports.yaml");
		Files.writeString(file.toPath(), yaml, StandardCharsets.UTF_8);
		ClasspathEntry entry = new ClasspathEntry(ConfigurationImportDeclarations.RESOURCE_PATH, file.toURI().toURL(), "test");
		return ConfigurationImportDeclarations.read(List.of(new ConfigurationImportDeclarations.DeclarationSource("test", entry))).get();
	}

	private VirtualEnvironment virtualEnvironment() {
		return new VirtualEnvironment() {
			@Override
			public String getEnv(String name) {
				return environment.get(name);
			}

			@Override
			public String getProperty(String name) {
				return systemProperties.get(name);
			}
		};
	}
}
