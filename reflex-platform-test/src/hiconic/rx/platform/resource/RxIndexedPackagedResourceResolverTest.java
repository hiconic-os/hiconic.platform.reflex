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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.braintribe.gm.config.assembly.model.ArtifactResourceSection;
import com.braintribe.gm.config.assembly.model.MaterializedResource;
import com.braintribe.gm.config.assembly.model.PackagedResourceIndex;
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

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
	public void explicitArtifactStillKeepsSiblingPathRelativeToTheConfiguration() {
		var resolver = rootResolver();
		var registry = new ValueDescriptorExpertRegistry();
		PackagedResourceValueDescriptorExperts.register(registry, resolver);
		var context = new StandardValueDescriptorEvaluationContext(registry).withAspect(ValueDescriptorSourceContext.class,
				new ValueDescriptorSourceContext("irrelevant-owner", "test-resources/config.yaml"));

		PackagedResourceText text = PackagedResourceText.T.create();
		text.setArtifact("reflex-platform-test");
		text.setPath("./assets/hello.txt");

		assertThat(context.<String> evaluate(text).get()).isEqualTo("public hello");

		PackagedResourceText absoluteText = PackagedResourceText.T.create();
		absoluteText.setArtifact("reflex-platform-test");
		absoluteText.setPath("/test-resources/assets/hello.txt");
		assertThat(context.<String> evaluate(absoluteText).get()).isEqualTo("public hello");
	}

	@Test
	public void readsUniqueAndRenamedMaterializedResourcesAsTextAndStream() throws Exception {
		Path conf = temporaryFolder.newFolder("integral-conf").toPath();
		Files.writeString(conf.resolve("unique.pem"), "unique content", StandardCharsets.UTF_8);
		Files.writeString(conf.resolve("shared--artifact-b.txt"), "content from b", StandardCharsets.UTF_8);

		PackagedResourceIndex index = PackagedResourceIndex.T.create();
		index.setArtifacts(Map.of(
				"group:artifact-a", section(resource("HICONIC-CONF/unique.pem", null)),
				"group:artifact-b", section(resource("HICONIC-CONF/shared.txt", "shared--artifact-b.txt"))));
		var resolver = new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);

		// Effective YAML without an artifact resolves through its synthetic compiled owner when the logical path is unique.
		try (var in = resolver.openStream("compiled", "HICONIC-CONF/unique.pem").get()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("unique content");
		}
		try (var in = resolver.resource("group:artifact-b", "HICONIC-CONF/shared.txt").asResource().openStream()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("content from b");
		}

		var registry = new ValueDescriptorExpertRegistry();
		PackagedResourceValueDescriptorExperts.register(registry, resolver);
		var context = new StandardValueDescriptorEvaluationContext(registry).withAspect(ValueDescriptorSourceContext.class,
				new ValueDescriptorSourceContext("compiled", "HICONIC-CONF/configuration.yaml"));
		PackagedResourceText uniqueText = PackagedResourceText.T.create();
		uniqueText.setPath("./unique.pem");
		assertThat(context.<String> evaluate(uniqueText).get()).isEqualTo("unique content");

		PackagedResourceText renamedText = PackagedResourceText.T.create();
		renamedText.setArtifact("group:artifact-b");
		renamedText.setPath("./shared.txt");
		assertThat(context.<String> evaluate(renamedText).get()).isEqualTo("content from b");
	}

	@Test
	public void resolvesMaterializedResourceByPlainArtifactId() throws Exception {
		Path conf = temporaryFolder.newFolder("integral-conf").toPath();
		Files.writeString(conf.resolve("shared--artifact-b.txt"), "content from b", StandardCharsets.UTF_8);

		PackagedResourceIndex index = PackagedResourceIndex.T.create();
		index.setArtifacts(Map.of("group:artifact-b", section(resource("HICONIC-CONF/shared.txt", "shared--artifact-b.txt"))));
		var resolver = new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);

		try (var in = resolver.resource("artifact-b", "HICONIC-CONF/shared.txt").asResource().openStream()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("content from b");
		}
	}

	/** Only one group contributes the artifactId, so the built source names just the artifactId, like on an ordinary classpath. */
	@Test
	public void buildsSourceWithPlainArtifactIdWhenUnambiguous() throws Exception {
		Path conf = temporaryFolder.newFolder("integral-conf").toPath();
		Files.writeString(conf.resolve("unique.pem"), "unique content", StandardCharsets.UTF_8);

		PackagedResourceIndex index = PackagedResourceIndex.T.create();
		index.setArtifacts(Map.of("group:artifact-a", section(resource("HICONIC-CONF/unique.pem", null))));
		var resolver = new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);

		assertThat(resolver.resource("HICONIC-CONF/unique.pem").asSource().getArtifact()).isEqualTo("artifact-a");
	}

	@Test
	public void requiresGroupIdWhenTwoGroupsContributeSameArtifactIdAndPath() throws Exception {
		var resolver = resolverWithSameArtifactIdInTwoGroups();

		assertThatThrownBy(() -> resolver.resource("configuration", "HICONIC-CONF/shared.txt")) //
				.isInstanceOf(IllegalArgumentException.class) //
				.hasMessageContaining("group.a:configuration") //
				.hasMessageContaining("group.b:configuration") //
				.hasMessageContaining("groupId:artifactId");

		try (var in = resolver.resource("group.b:configuration", "HICONIC-CONF/shared.txt").asResource().openStream()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("content from b");
		}
	}

	/** A source built for an artifactId that two groups contribute names the groupId, so that it can be resolved again. */
	@Test
	public void buildsSourceWithGroupIdWhenArtifactIdIsAmbiguous() throws Exception {
		var resolver = resolverWithSameArtifactIdInTwoGroups();

		PackagedSource source = resolver.resource("HICONIC-CONF/only-a.txt").asSource();

		assertThat(source.getArtifact()).isEqualTo("group.a:configuration");
		try (var in = resolver.resource(source.getArtifact(), source.getPath()).asResource().openStream()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("only in a");
		}
	}

	/**
	 * An assembled application knows the groupId of the packaged resources, but not of the effective configuration. That is one artifact, not two, so
	 * the built source still names just the artifactId.
	 */
	@Test
	public void buildsSourceWithPlainArtifactIdWhenOnlyOneSourceKnowsTheGroupId() throws Exception {
		Path packagedResources = temporaryFolder.newFolder("packaged-resources").toPath();
		Files.createDirectories(packagedResources.resolve("configuration-1.0/icons"));
		Files.writeString(packagedResources.resolve("configuration-1.0/icons/logo.svg"), "<svg/>", StandardCharsets.UTF_8);
		Files.writeString(packagedResources.resolve("index.properties"), """
				formatVersion=1
				artifact.count=1
				artifact.0.folder=configuration-1.0
				artifact.0.groupId=example
				artifact.0.artifactId=configuration
				artifact.0.resource.count=1
				artifact.0.resource.0.path=icons/logo.svg
				""", StandardCharsets.UTF_8);

		Path effectiveConf = temporaryFolder.newFolder("effective-conf").toPath();
		Files.createDirectories(effectiveConf.resolve("configuration"));
		Files.writeString(effectiveConf.resolve("configuration/configuration.yaml"), "value: effective", StandardCharsets.UTF_8);

		var index = new ClasspathIndex(List.of( //
				ClasspathIndex.filesystemSource(packagedResources, ""), //
				ClasspathIndex.filesystemSlots(effectiveConf, "HICONIC-CONF")));
		var resolver = new RxIndexedPackagedResourceResolver(index, "");

		assertThat(resolver.resource("icons/logo.svg").asSource().getArtifact()).isEqualTo("configuration");
		assertThat(resolver.resource("HICONIC-CONF/configuration.yaml").asSource().getArtifact()).isEqualTo("configuration");
	}

	private RxIndexedPackagedResourceResolver resolverWithSameArtifactIdInTwoGroups() throws Exception {
		Path conf = temporaryFolder.newFolder("integral-conf").toPath();
		Files.writeString(conf.resolve("shared--a.txt"), "content from a", StandardCharsets.UTF_8);
		Files.writeString(conf.resolve("shared--b.txt"), "content from b", StandardCharsets.UTF_8);
		Files.writeString(conf.resolve("only-a.txt"), "only in a", StandardCharsets.UTF_8);

		PackagedResourceIndex index = PackagedResourceIndex.T.create();
		index.setArtifacts(Map.of(
				"group.a:configuration", section(resource("HICONIC-CONF/shared.txt", "shared--a.txt"), resource("HICONIC-CONF/only-a.txt", null)),
				"group.b:configuration", section(resource("HICONIC-CONF/shared.txt", "shared--b.txt"))));
		return new RxIndexedPackagedResourceResolver(new ClasspathIndex(), "", index, conf);
	}

	private static ArtifactResourceSection section(MaterializedResource... resources) {
		ArtifactResourceSection section = ArtifactResourceSection.T.create();
		section.setResources(List.of(resources));
		return section;
	}

	private static MaterializedResource resource(String path, String materializedAs) {
		MaterializedResource resource = MaterializedResource.T.create();
		resource.setPath(path);
		resource.setMaterializedAs(materializedAs);
		return resource;
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
