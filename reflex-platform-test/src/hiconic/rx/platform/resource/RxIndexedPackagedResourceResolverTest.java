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

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;
import static com.braintribe.testing.junit.assertions.gm.assertj.core.api.GmAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.bvd.resource.PackagedResource;
import com.braintribe.model.bvd.resource.PackagedResourceText;
import com.braintribe.model.generic.session.exception.GmSessionRuntimeException;
import com.braintribe.model.processing.resource.packaged.PackagedResourceValueDescriptorExperts;
import com.braintribe.model.processing.vde.reasoned.api.ValueDescriptorSourceContext;
import com.braintribe.model.processing.vde.reasoned.impl.StandardValueDescriptorEvaluationContext;
import com.braintribe.model.processing.vde.reasoned.impl.ValueDescriptorExpertRegistry;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.PackagedSource;

import hiconic.rx.module.api.resource.RxPackagedResourceResolver;
import hiconic.rx.platform.processing.resource.RxIndexedPackagedResourceResolver;

public class RxIndexedPackagedResourceResolverTest {

	@Test
	public void cachesMetadataButReturnsIndependentResourcesAndStreams() throws Exception {
		var resolver = scopedResolver();
		Resource plain = resolver.resource("assets/hello.txt").asResource();
		assertThat(plain.getMimeType()).isNull();
		assertThat(plain.getMd5()).isNull();

		Resource enriched = resolver.resource("assets/hello.txt").withHttpMetadata().asResource();
		assertThat(enriched).isNotSameAs(plain);
		assertThat(enriched.getMimeType()).isEqualTo("text/plain");
		assertThat(enriched.getFileSize()).isPositive();
		assertThat(enriched.getMd5()).isEqualTo("d231e00f039349ca5f4ef6197be2a438");
		enriched.setMimeType("tampered/type");
		Resource independentlyBuilt = resolver.resource("assets/hello.txt").withHttpMetadata().asResource();
		assertThat(independentlyBuilt).isNotSameAs(enriched);
		assertThat(independentlyBuilt.getMimeType()).isEqualTo("text/plain");

		try (var first = enriched.openStream(); var second = enriched.openStream()) {
			assertThat(first).isNotSameAs(second);
			assertThat(new String(first.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("public hello");
			assertThat(second.readAllBytes()).isEqualTo("public hello".getBytes(StandardCharsets.UTF_8));
		}
	}

	@Test
	public void inventoriesLogicalDirectoriesPerFolder() {
		var scoped = scopedResolver();
		assertThat(scoped.inventory().resourcePaths())
				.containsExactlyInAnyOrder("test/hello.txt", "assets/hello.txt", "assets/nested/second.txt");
		assertThat(scoped.inventory().list("assets")).extracting(entry -> entry.name() + ":" + entry.directory())
				.containsExactly("hello.txt:false", "nested:true");

		var rootResolver = rootResolver();
		assertThat(rootResolver.inventory().resourcePaths()).contains("test-resources/test/hello.txt").doesNotContain("test/hello.txt");
	}

	@Test
	public void buildsModeledReferencesThatArePersistableAndReadableAtOnce() throws Exception {
		var resolver = rootResolver();
		Resource resource = resolver.resource("test-resources/test/hello.txt").withMimeType().asResource();

		assertThat(resource.isTransient()).isFalse();
		assertThat(resource.getMimeType()).isEqualTo("text/plain");
		assertThat(resource.getResourceSource()).isInstanceOf(PackagedSource.class);

		// A source always carries the artifact and the full artifact relative path, so that it says where the file is without any further context.
		PackagedSource source = (PackagedSource) resource.getResourceSource();
		assertThat(source.getArtifact()).isEqualTo("reflex-platform-test");
		assertThat(source.getPath()).isEqualTo("test-resources/test/hello.txt");

		// The very same Resource can be read without a session, because the resolver attached a reader to the address.
		try (var in = resource.openStream()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("private hello");
		}
	}

	/** The reader is transient, so an address that came back from an access or a file carries none. Such a Resource is read through its session. */
	@Test
	public void addressWithoutReaderIsNotStreamableByItself() {
		var resolver = rootResolver();
		Resource resource = resolver.resource("test-resources/test/hello.txt").asResource();

		PackagedSource source = (PackagedSource) resource.getResourceSource();
		source.setInputStreamProvider(null);

		assertThatThrownBy(resource::openStream).isInstanceOf(GmSessionRuntimeException.class);
	}

	@Test
	public void resolvesAnyIndexedResourceByArtifactAndFullArtifactRelativePath() throws Exception {
		var resolver = rootResolver();
		Resource resource = resolver.resource("reflex-platform-test", "test-resources/assets/hello.txt").asResource();

		PackagedSource source = (PackagedSource) resource.getResourceSource();
		assertThat(source.getArtifact()).isEqualTo("reflex-platform-test");
		assertThat(source.getPath()).isEqualTo("test-resources/assets/hello.txt");
		try (var in = resolver.resource(source).asHandle().asStream()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("public hello");
		}
	}

	@Test
	public void evaluatesResourcesRelativeToTheOwningConfigurationEntry() {
		var resolver = rootResolver();
		var registry = new ValueDescriptorExpertRegistry();
		PackagedResourceValueDescriptorExperts.register(registry, resolver);
		var context = new StandardValueDescriptorEvaluationContext(registry) //
				.withAspect(ValueDescriptorSourceContext.class,
						new ValueDescriptorSourceContext("reflex-platform-test", "test-resources/config.yaml"));

		PackagedResourceText importText = PackagedResourceText.T.create();
		importText.setPath("./assets/hello.txt");

		Maybe<String> textMaybe = context.<String> evaluate(importText);
		assertThat(textMaybe).isSatisfied();
		assertThat(textMaybe.get()).isEqualTo("public hello");

		PackagedResource packagedResource = PackagedResource.T.create();
		packagedResource.setPath("./assets/hello.txt");
		Resource resource = context.<Resource> evaluate(packagedResource).get();
		PackagedSource source = (PackagedSource) resource.getResourceSource();
		assertThat(source.getArtifact()).isEqualTo("reflex-platform-test");
		assertThat(source.getPath()).isEqualTo("test-resources/assets/hello.txt");
	}

	@Test
	public void rejectsMissingAndUnsafePaths() {
		var resolver = scopedResolver();
		assertThat(resolver.inventory().contains("../assets/hello.txt")).isFalse();
		assertThatThrownBy(() -> resolver.resource("missing.txt")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> resolver.resource("../assets/hello.txt")).isInstanceOf(IllegalArgumentException.class);
	}

	private RxIndexedPackagedResourceResolver rootResolver() {
		return new RxIndexedPackagedResourceResolver(new ClasspathIndex(getClass().getClassLoader()), "");
	}

	/** A resolver scoped to one folder. The folder has no meaning to the platform; it is simply the one this test owns. */
	private RxPackagedResourceResolver scopedResolver() {
		return rootResolver().below("test-resources");
	}
}
