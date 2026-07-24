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
package hiconic.rx.platform.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.model.resource.Resource;

import hiconic.rx.module.api.wire.RxPackagedPublicResourcesContract;
import hiconic.rx.module.api.wire.RxPackagedResourcesContract;
import hiconic.rx.platform.processing.resource.RxIndexedPackagedResourceResolver;
import hiconic.rx.resource.model.packaged.PackagedResourceNamespace;
import hiconic.rx.resource.model.packaged.PackagedResourceSource;

public class RxIndexedPackagedResourceResolverTest {

	@Test
	public void cachesMetadataButReturnsIndependentResourcesAndStreams() throws Exception {
		var resolver = resolver(RxPackagedPublicResourcesContract.CLASSPATH_ROOT);
		Resource plain = resolver.resource("assets/hello.txt").asResource();
		assertThat(plain.getMimeType()).isNull();
		assertThat(plain.getMd5()).isNull();

		Resource enriched = resolver.resource("assets/hello.txt").withHttpMetadata().asResource();
		assertThat(enriched).isNotSameAs(plain);
		assertThat(enriched.getMimeType()).isEqualTo("text/plain");
		assertThat(enriched.getFileSize()).isPositive();
		assertThat(enriched.getMd5()).isEqualTo("03abb9cf6bf47490382274606f3012cd");
		enriched.setMimeType("tampered/type");
		Resource independentlyBuilt = resolver.resource("assets/hello.txt").withHttpMetadata().asResource();
		assertThat(independentlyBuilt).isNotSameAs(enriched);
		assertThat(independentlyBuilt.getMimeType()).isEqualTo("text/plain");

		try (var first = enriched.openStream(); var second = enriched.openStream()) {
			assertThat(first).isNotSameAs(second);
			assertThat(new String(first.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("public hello\n");
			assertThat(second.readAllBytes()).isEqualTo("public hello\n".getBytes(StandardCharsets.UTF_8));
		}
	}

	@Test
	public void inventoriesLogicalDirectoriesAndSeparatesPrivateFromPublic() {
		var publicResolver = resolver(RxPackagedPublicResourcesContract.CLASSPATH_ROOT);
		assertThat(publicResolver.inventory().resourcePaths()).containsExactlyInAnyOrder(
				"assets/hello.txt", "assets/nested/second.txt");
		assertThat(publicResolver.inventory().list("assets"))
				.extracting(entry -> entry.name() + ":" + entry.directory())
				.containsExactly("hello.txt:false", "nested:true");

		var privateResolver = resolver(RxPackagedResourcesContract.CLASSPATH_ROOT);
		assertThat(privateResolver.inventory().resourcePaths()).contains("test/hello.txt").doesNotContain("assets/hello.txt");
	}

	@Test
	public void buildsModeledPersistableReferencesWithoutTransientStreamState() {
		var resolver = resolver(RxPackagedResourcesContract.CLASSPATH_ROOT);
		Resource resource = resolver.resource("test/hello.txt").withMimeType().asPersistableResource();

		assertThat(resource.isTransient()).isFalse();
		assertThat(resource.getMimeType()).isEqualTo("text/plain");
		assertThat(resource.getResourceSource()).isInstanceOf(PackagedResourceSource.class);
		PackagedResourceSource source = (PackagedResourceSource) resource.getResourceSource();
		assertThat(source.getPath()).isEqualTo("test/hello.txt");
		assertThat(source.getNamespace()).isEqualTo(PackagedResourceNamespace.resources);
	}

	@Test
	public void rejectsMissingAndUnsafePaths() {
		var resolver = resolver(RxPackagedPublicResourcesContract.CLASSPATH_ROOT);
		assertThat(resolver.inventory().contains("../assets/hello.txt")).isFalse();
		assertThatThrownBy(() -> resolver.resource("missing.txt")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> resolver.resource("../assets/hello.txt")).isInstanceOf(IllegalArgumentException.class);
	}

	private RxIndexedPackagedResourceResolver resolver(String root) {
		PackagedResourceNamespace namespace = root.equals(RxPackagedPublicResourcesContract.CLASSPATH_ROOT)
				? PackagedResourceNamespace.publicResources
				: PackagedResourceNamespace.resources;
		return new RxIndexedPackagedResourceResolver(new ClasspathIndex(getClass().getClassLoader()), root,
				namespace);
	}
}
