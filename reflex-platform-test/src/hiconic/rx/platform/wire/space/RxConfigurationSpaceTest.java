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

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.config.yaml.ModeledYamlConfiguration;

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
	}
}
