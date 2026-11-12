package hiconic.rx.platform.loading;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.Test;

import com.braintribe.common.artifact.ArtifactReflection;

public class ApplicationArtifactReflectionResolverTest {

	@Test
	public void markerSelectsReflectionFromItsOwnJar() throws Exception {
		Path jar = artifactJar(true);
		try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[] { jar.toUri().toURL() }, getClass().getClassLoader())) {
			ArtifactReflection reflection = ApplicationArtifactReflectionResolver.resolve(classLoader, null);
			assertThat(reflection.versionedName()).isEqualTo("example.group:sample-app#1.2.3-rc");
		}
	}

	@Test
	public void ideMarkerSelectsReflectionFromItsProject() throws Exception {
		Path project = Files.createTempDirectory("rx-application-ide");
		Files.createFile(project.resolve("pom.xml"));
		Path classes = Files.createDirectories(project.resolve("classes/META-INF"));
		Files.createFile(classes.resolve("rx-application-artifact.marker"));
		Path classGen = Files.createDirectories(project.resolve("class-gen/META-INF"));
		Files.writeString(classGen.resolve("artifact-descriptor.properties"), descriptor());
		try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[] { project.resolve("classes").toUri().toURL(),
				project.resolve("class-gen").toUri().toURL() }, getClass().getClassLoader())) {
			ArtifactReflection reflection = ApplicationArtifactReflectionResolver.resolve(classLoader, null);
			assertThat(reflection.versionedName()).isEqualTo("example.group:sample-app#1.2.3-rc");
		}
	}

	@Test
	public void syntheticApplicationSelectsConfiguredDependency() throws Exception {
		Path jar = artifactJar(false);
		try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[] { jar.toUri().toURL() }, getClass().getClassLoader())) {
			ArtifactReflection reflection = ApplicationArtifactReflectionResolver.resolve(classLoader, "example.group:sample-app");
			assertThat(reflection.versionedName()).isEqualTo("example.group:sample-app#1.2.3-rc");
		}
	}

	@Test
	public void markerAndPropertyMustAgree() throws Exception {
		Path jar = artifactJar(true);
		try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[] { jar.toUri().toURL() }, getClass().getClassLoader())) {
			assertThatThrownBy(() -> ApplicationArtifactReflectionResolver.resolve(classLoader, "example.group:another-app"))
					.isInstanceOf(IllegalStateException.class).hasMessageContaining("conflicts");
		}
	}

	private static Path artifactJar(boolean marker) throws Exception {
		Path jar = Files.createTempFile("rx-application-identity", ".jar");
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
			if (marker) {
				out.putNextEntry(new JarEntry(ApplicationArtifactReflectionResolver.MARKER_PATH));
				out.closeEntry();
			}
			out.putNextEntry(new JarEntry("META-INF/artifact-descriptor.properties"));
			out.write(descriptor().getBytes(java.nio.charset.StandardCharsets.UTF_8));
			out.closeEntry();
		}
		return jar;
	}

	private static String descriptor() {
		return "groupId=example.group\nartifactId=sample-app\nversion=1.2.3-rc\nreflection-class=" + SampleArtifact.class.getName() + "\n";
	}

	public static class SampleArtifact {
		public static final ArtifactReflection reflection = new ArtifactReflection() {
			@Override public String groupId() { return "example.group"; }
			@Override public String artifactId() { return "sample-app"; }
			@Override public String version() { return "1.2.3-rc"; }
			@Override public Set<String> archetypes() { return Set.of(); }
			@Override public String name() { return "example.group:sample-app"; }
			@Override public String versionedName() { return name() + "#" + version(); }
		};
	}
}
