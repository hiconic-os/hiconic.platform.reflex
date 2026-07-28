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
}
