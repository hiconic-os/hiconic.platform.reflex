package hiconic.rx.platform.conf;

import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.config.yaml.PropertyResolutions;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.config.PropertyNotFound;
import com.braintribe.gm.model.reason.config.UnresolvedProperty;
import com.braintribe.utils.encryption.Cryptor;
import com.braintribe.ve.api.VirtualEnvironment;

import hiconic.rx.platform.model.configuration.ReflexAppConfiguration;
import hiconic.rx.platform.processing.resource.RxIndexedPackagedResourceResolver;
import hiconic.rx.resource.model.packaged.PackagedResourceNamespace;

/**
 * Tests for {@link RxPropertyResolver}.
 */
public class RxPropertyResolverTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private RxPropertyResolver resolver;
	private final Map<String, String> rawProperties = newMap();
	private final Map<String, String> envVars = newMap();
	private final Map<String, String> systemProps = newMap();

	@Before
	public void setup() {
		resolver = new RxPropertyResolver();
		resolver.setVirtualEnvironment(new TestVirtualEnvironment());
		resolver.setRawProperties(rawProperties);
	}

	class TestVirtualEnvironment implements VirtualEnvironment {
		// @formatter:off
		@Override public String getEnv(String name) { return envVars.get(name); }
		@Override public String getProperty(String name) { return systemProps.get(name); }
		// @formatter:on
	}

	// Tests

	@Test
	public void testResolveStaticValue() {
		rawProperties.put("db.host", "localhost");
		assertThat(resolver.resolve("db.host")).isEqualTo("localhost");
	}

	@Test
	public void testResolveReturnsNullForMissingProperty() {
		assertThat(resolver.resolve("nonexistent")).isNull();
	}

	@Test
	public void testResolveFallsBackToSystemProperty() {
		systemProps.put("sys.prop", "sysValue");
		assertThat(resolver.resolve("sys.prop")).isEqualTo("sysValue");
	}

	@Test
	public void testResolveFallsBackToEnvVar() {
		envVars.put("ENV_VAR", "envValue");
		assertThat(resolver.resolve("ENV_VAR")).isEqualTo("envValue");
	}

	@Test
	public void testRawPropertyTakesPrecedenceOverSystemProperty() {
		rawProperties.put("key", "rawValue");
		systemProps.put("key", "sysValue");
		assertThat(resolver.resolve("key")).isEqualTo("rawValue");
	}

	@Test
	public void testSystemPropertyTakesPrecedenceOverEnvVar() {
		systemProps.put("key", "sysValue");
		envVars.put("key", "envValue");
		assertThat(resolver.resolve("key")).isEqualTo("sysValue");
	}

	@Test
	public void testManagedModeDoesNotFallBackToHostProperties() {
		systemProps.put("SYSTEM_VALUE", "hidden");
		envVars.put("ENV_VALUE", "hidden");
		resolver.setManagedPropertiesOnly(true);

		assertThat(resolver.resolve("SYSTEM_VALUE")).isNull();
		assertThat(resolver.resolve("ENV_VALUE")).isNull();
	}

	@Test
	public void testManagedModeResolvesMaterializedImportsLikeNormalProperties() {
		rawProperties.put("IMPORTED_VALUE", "managed");
		resolver.setManagedPropertiesOnly(true);

		assertThat(resolver.resolve("IMPORTED_VALUE")).isEqualTo("managed");
	}

	// ENV_PREFIX

	@Test
	public void testEnvPrefixResolvesFromEnvironment() {
		envVars.put("MY_VAR", "fromEnv");
		assertThat(resolver.resolve(PropertyResolutions.ENV_PREFIX + "MY_VAR")).isEqualTo("fromEnv");
	}

	// --- placeholder resolution ---

	@Test
	public void testPlaceholderSubstitution() {
		rawProperties.put("HOST", "localhost");
		rawProperties.put("URL", "http://${HOST}:8080");
		assertThat(resolver.resolve("URL")).isEqualTo("http://localhost:8080");
	}

	@Test
	public void testNestedPlaceholderSubstitution() {
		rawProperties.put("a", "world");
		rawProperties.put("b", "hello ${a}");
		rawProperties.put("c", "${b}!");
		assertThat(resolver.resolve("c")).isEqualTo("hello world!");
	}

	@Test
	public void testUnresolvedPlaceholderReturnsUnsatisfied() {
		rawProperties.put("key", "${missing}");
		Maybe<String> result = resolver.resolveReasoned("key");
		assertThat(result.isUnsatisfied()).isTrue();
		assertThat((Object) result.whyUnsatisfied()).isInstanceOf(UnresolvedProperty.class);
	}

	// --- resolveReasoned() tests ---

	@Test
	public void testResolveReasonedSatisfied() {
		rawProperties.put("key", "value");
		Maybe<String> result = resolver.resolveReasoned("key");
		assertThat(result.isSatisfied()).isTrue();
		assertThat(result.get()).isEqualTo("value");
	}

	@Test
	public void testResolveReasonedPropertyNotFound() {
		Maybe<String> result = resolver.resolveReasoned("missing");
		assertThat(result.isUnsatisfied()).isTrue();
		assertThat((Object) result.whyUnsatisfied()).isInstanceOf(PropertyNotFound.class);
	}

	// --- caching ---

	@Test
	public void testResolvedValueIsCached() {
		rawProperties.put("key", "value");
		Maybe<String> first = resolver.resolveReasoned("key");
		Maybe<String> second = resolver.resolveReasoned("key");
		assertThat(first).isSameAs(second);
	}

	// --- decrypt function ---

	@Test
	public void testDecryptPlaceholder() {
		String secret = "mySecret";
		String plainText = "sensitiveData";
		String encrypted = Cryptor.encrypt(secret, null, null, null, plainText);

		rawProperties.put("RX_DECRYPT_SECRET", secret);
		rawProperties.put("password", "${decrypt('" + encrypted + "')}");

		String resolved = resolver.resolve("password");
		assertThat(resolved).isEqualTo(plainText);
	}

	@Test
	public void testDirectDecryptPlaceholderContent() {
		String secret = "mySecret";
		String plainText = "sensitiveData";
		String encrypted = Cryptor.encrypt(secret, null, null, null, plainText);

		rawProperties.put("RX_DECRYPT_SECRET", secret);

		Maybe<String> resolved = resolver.resolvePlaceholderReasoned("decrypt('" + encrypted + "')");
		assertThat(resolved.isSatisfied()).isTrue();
		assertThat(resolved.get()).isEqualTo(plainText);
	}

	@Test
	public void testDecryptInModeledYamlConfiguration() throws Exception {
		String secret = "mySecret";
		String plainText = "sensitiveData";
		String encrypted = Cryptor.encrypt(secret, null, null, null, plainText);

		rawProperties.put("RX_DECRYPT_SECRET", secret);

		File configFolder = temporaryFolder.newFolder("conf");
		Files.writeString(new File(configFolder, "reflex-app-configuration.yaml").toPath(), //
				"applicationId: ${decrypt('" + encrypted + "')}\n", StandardCharsets.UTF_8);

		ModeledYamlConfiguration modeledConfiguration = new ModeledYamlConfiguration();
		modeledConfiguration.setConfigFolder(configFolder);
		modeledConfiguration.setExternalReasonedPropertyLookup(resolver::resolvePlaceholderReasoned);
		modeledConfiguration.setValueDescriptorExpressionCodec(RxConfigurationValueDescriptorExperts.expressionCodec());
		var resources = new RxIndexedPackagedResourceResolver(new ClasspathIndex(getClass().getClassLoader()), "HICONIC-RESOURCES",
				PackagedResourceNamespace.resources);
		modeledConfiguration.setValueDescriptorExpertConfigurer(
				registry -> RxConfigurationValueDescriptorExperts.register(registry, resources, resolver));

		ReflexAppConfiguration config = modeledConfiguration.config(ReflexAppConfiguration.T);
		assertThat(config.getApplicationId()).isEqualTo(plainText);
	}

	@Test
	public void testDecryptWithDoubleQuotes() {
		String secret = "mySecret";
		String plainText = "sensitiveData";
		String encrypted = Cryptor.encrypt(secret, null, null, null, plainText);

		rawProperties.put("RX_DECRYPT_SECRET", secret);
		rawProperties.put("password", "${decrypt(\"" + encrypted + "\")}");

		String resolved = resolver.resolve("password");
		assertThat(resolved).isEqualTo(plainText);
	}

	@Test
	public void testDecryptWithMissingSecretReturnsUnsatisfied() {
		rawProperties.put("password", "${decrypt('someEncryptedValue')}");
		Maybe<String> result = resolver.resolveReasoned("password");
		assertThat(result.isUnsatisfied()).isTrue();
	}

	@Test
	public void testDecryptWithWrongSecretReturnsUnsatisfied() {
		rawProperties.put("RX_DECRYPT_SECRET", "wrongSecret");
		String encrypted = Cryptor.encrypt("correctSecret", null, null, null, "data");
		rawProperties.put("password", "${decrypt('" + encrypted + "')}");

		Maybe<String> result = resolver.resolveReasoned("password");
		assertThat(result.isUnsatisfied()).isTrue();
	}

	// --- unsupported operation ---

	@Test
	public void testUnsupportedFunctionReturnsUnsatisfied() {
		rawProperties.put("key", "${unknownFunc('param')}");
		Maybe<String> result = resolver.resolveReasoned("key");
		assertThat(result.isUnsatisfied()).isTrue();
	}

	// --- multiple placeholders ---

	@Test
	public void testMultiplePlaceholders() {
		rawProperties.put("host", "localhost");
		rawProperties.put("port", "8080");
		rawProperties.put("url", "http://${host}:${port}/api");
		assertThat(resolver.resolve("url")).isEqualTo("http://localhost:8080/api");
	}

	// --- placeholder resolved from env ---

	@Test
	public void testPlaceholderResolvedFromEnvVar() {
		envVars.put("DB_HOST", "prodhost");
		rawProperties.put("url", "jdbc://${DB_HOST}:5432");
		assertThat(resolver.resolve("url")).isEqualTo("jdbc://prodhost:5432");
	}
}
