// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.platform.wire.space;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.Test;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;
import com.braintribe.gm.config.yaml.ModeledYamlConfigurationLoader;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.PlaceholderSupport;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionCodecOption;
import com.braintribe.model.processing.vde.expression.api.ValueDescriptorExpressionProjectionOption;
import com.braintribe.model.processing.vde.reasoned.api.ValueDescriptorSourceContext;

import hiconic.rx.platform.processing.resource.PackagedResourceValueDescriptorExperts;
import hiconic.rx.platform.processing.resource.RxIndexedPackagedResourceResolver;
import hiconic.rx.platform.resource.model.ResourceExpressionConfiguration;
import hiconic.rx.resource.model.packaged.PackagedResourceNamespace;
import hiconic.rx.resource.model.packaged.PackagedResourceSource;

public class RxConfigurationSpaceTest {

	@Test
	public void resolvesCanonicalIndexedResource() throws Exception {
		var index = new ClasspathIndex(getClass().getClassLoader());
		var resource = RxConfigurationSpace.resolveIndexedClasspathResource(index, "/HICONIC-RESOURCES/test/hello.txt");

		try (var in = resource.asStream()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("private hello\n");
		}
	}

	@Test
	public void rejectsMissingAndUnsafePaths() {
		var index = new ClasspathIndex(getClass().getClassLoader());

		assertThatThrownBy(() -> RxConfigurationSpace.resolveIndexedClasspathResource(index, "missing/resource"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not found");
		assertThatThrownBy(() -> RxConfigurationSpace.resolveIndexedClasspathResource(index, "../outside"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid");
	}

	@Test
	public void evaluatesRelativePackagedResourcesThroughModeledConfiguration() {
		var index = new ClasspathIndex(getClass().getClassLoader());
		var resolver = new RxIndexedPackagedResourceResolver(index, "HICONIC-RESOURCES", PackagedResourceNamespace.resources);
		var configuration = new ModeledYamlConfiguration();
		configuration.setClasspathIndex(index);
		configuration.setClasspathConfPath("HICONIC-CONF");
		configuration.setValueDescriptorExpressionCodec(PackagedResourceValueDescriptorExperts.expressionCodec());
		configuration.setValueDescriptorExpertConfigurer(
				registry -> PackagedResourceValueDescriptorExperts.register(registry, resolver));

		ResourceExpressionConfiguration loaded = configuration.config(ResourceExpressionConfiguration.T);

		assertThat(loaded.getText()).isEqualTo("public hello\n");
		PackagedResourceSource source = (PackagedResourceSource) loaded.getResource().getResourceSource();
		assertThat(source.getArtifact()).isEqualTo("reflex-platform-test");
		assertThat(source.getPath()).isEqualTo("HICONIC-PUBLIC-RESOURCES/assets/hello.txt");
		loaded.getResource().setCreator("configuration-curator");
		loaded.getResource().setTags(Set.of("branding", "stable"));

		var outputContext = new ValueDescriptorSourceContext("reflex-platform-test",
				"HICONIC-CONF/resource-expression-configuration.test.yaml");
		var options = GmSerializationOptions.deriveDefaults()
				.inferredRootType(ResourceExpressionConfiguration.T)
				.set(PlaceholderSupport.class, true)
				.set(ValueDescriptorExpressionCodecOption.class, PackagedResourceValueDescriptorExperts.expressionCodec())
				.set(ValueDescriptorExpressionProjectionOption.class, PackagedResourceValueDescriptorExperts.projection(outputContext))
				.build();
		StringWriter writer = new StringWriter();
		new YamlMarshaller().marshall(writer, loaded, options);
		assertThat(writer.toString())
				.contains("resourceSource: \"${artifactResourceSource('../HICONIC-PUBLIC-RESOURCES/assets/hello.txt')}\"")
				.contains("creator: \"configuration-curator\"")
				.contains("branding")
				.doesNotContain("resource: \"${artifactResource(");

		ResourceExpressionConfiguration roundtripped = new ModeledYamlConfigurationLoader()
				.valueDescriptorExpressions(PackagedResourceValueDescriptorExperts.expressionCodec())
				.valueDescriptorExperts(registry -> PackagedResourceValueDescriptorExperts.register(registry, resolver))
				.valueDescriptorAspect(ValueDescriptorSourceContext.class, outputContext)
				.loadConfig(ResourceExpressionConfiguration.T,
						() -> new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8)))
				.get();
		assertThat(roundtripped.getResource().getCreator()).isEqualTo("configuration-curator");
		assertThat(roundtripped.getResource().getTags()).containsExactlyInAnyOrder("branding", "stable");
		PackagedResourceSource roundtrippedSource = (PackagedResourceSource) roundtripped.getResource().getResourceSource();
		assertThat(roundtrippedSource.getArtifact()).isEqualTo("reflex-platform-test");
		assertThat(roundtrippedSource.getPath()).isEqualTo("HICONIC-PUBLIC-RESOURCES/assets/hello.txt");

		var compactOptions = GmSerializationOptions.deriveDefaults()
				.inferredRootType(ResourceExpressionConfiguration.T)
				.set(PlaceholderSupport.class, true)
				.set(ValueDescriptorExpressionCodecOption.class, PackagedResourceValueDescriptorExperts.expressionCodec())
				.set(ValueDescriptorExpressionProjectionOption.class,
						PackagedResourceValueDescriptorExperts.projection(outputContext,
								resource -> "configuration-curator".equals(resource.getCreator())))
				.build();
		StringWriter compactWriter = new StringWriter();
		new YamlMarshaller().marshall(compactWriter, loaded, compactOptions);
		assertThat(compactWriter.toString())
				.contains("resource: \"${artifactResource('../HICONIC-PUBLIC-RESOURCES/assets/hello.txt')}\"")
				.doesNotContain("configuration-curator");
	}
}
