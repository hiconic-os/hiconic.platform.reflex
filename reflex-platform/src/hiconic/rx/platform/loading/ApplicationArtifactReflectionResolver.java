// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.loading;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.rx.platform.conf.RxPropertyResolver;

/** Resolves the application's selected artifact independently of the synthetic packaging artifact. */
public final class ApplicationArtifactReflectionResolver {

	public static final String MARKER_PATH = "META-INF/rx-application-artifact.marker";
	public static final String ARTIFACT_PROPERTY = "reflex.application.artifact";
	private static final String DESCRIPTOR_PATH = "META-INF/artifact-descriptor.properties";

	private ApplicationArtifactReflectionResolver() {
	}

	public static ArtifactReflection resolve(RxPropertyResolver properties) {
		return resolve(ApplicationArtifactReflectionResolver.class.getClassLoader(), properties.resolve(ARTIFACT_PROPERTY));
	}

	static ArtifactReflection resolve(ClassLoader classLoader, String configuredArtifact) {
		List<URL> markers = resources(classLoader, MARKER_PATH);
		if (markers.size() > 1)
			throw new IllegalStateException("Multiple RX application artifact markers found: " + markers);

		String selection = configuredArtifact == null ? null : configuredArtifact.trim();
		if (selection != null && selection.isEmpty())
			throw new IllegalStateException("Empty " + ARTIFACT_PROPERTY + " property");

		if (!markers.isEmpty()) {
			ArtifactReflection reflection = fromMarker(markers.get(0), classLoader);
			if (selection != null && !selection.equals(reflection.name()))
				throw new IllegalStateException("RX application artifact marker " + reflection.name() + " conflicts with "
						+ ARTIFACT_PROPERTY + "=" + selection);
			return reflection;
		}

		if (selection == null)
			return null;

		return fromClasspathDescriptor(classLoader, selection);
	}

	private static ArtifactReflection fromMarker(URL marker, ClassLoader classLoader) {
		try {
			URL descriptor;
			if ("jar".equals(marker.getProtocol())) {
				JarURLConnection connection = (JarURLConnection) marker.openConnection();
				descriptor = new URL("jar:" + connection.getJarFileURL().toExternalForm() + "!/" + DESCRIPTOR_PATH);
			} else if ("file".equals(marker.getProtocol())) {
				Path markerPath = Path.of(marker.toURI());
				Path output = markerPath.getParent().getParent();
				Path descriptorPath = output.resolve(DESCRIPTOR_PATH);
				if (!Files.isRegularFile(descriptorPath)) {
					Path project = findProject(output);
					if (project == null)
						throw new IllegalStateException("Cannot find project for RX application marker " + marker);
					descriptorPath = project.resolve("class-gen").resolve(DESCRIPTOR_PATH);
				}
				descriptor = descriptorPath.toUri().toURL();
			} else {
				throw new IllegalStateException("Unsupported RX application marker URL: " + marker);
			}
			return readReflection(descriptor, classLoader);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot resolve artifact reflection from RX application marker " + marker, e);
		}
	}

	private static Path findProject(Path output) {
		for (Path path = output; path != null; path = path.getParent())
			if (Files.isRegularFile(path.resolve("pom.xml")))
				return path;
		return null;
	}

	private static ArtifactReflection fromClasspathDescriptor(ClassLoader classLoader, String selection) {
		String[] parts = selection.split(":", -1);
		if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank())
			throw new IllegalStateException("Invalid " + ARTIFACT_PROPERTY + " value [" + selection + "]; expected groupId:artifactId");

		List<URL> matches = new ArrayList<>();
		for (URL descriptor : resources(classLoader, DESCRIPTOR_PATH)) {
			Properties properties = readProperties(descriptor);
			if (parts[0].equals(properties.getProperty("groupId")) && parts[1].equals(properties.getProperty("artifactId")))
				matches.add(descriptor);
		}
		if (matches.size() != 1)
			throw new IllegalStateException("Expected one artifact descriptor for RX application " + selection + ", found " + matches);
		return readReflection(matches.get(0), classLoader);
	}

	private static List<URL> resources(ClassLoader classLoader, String path) {
		try {
			Enumeration<URL> enumeration = classLoader.getResources(path);
			List<URL> result = new ArrayList<>();
			while (enumeration.hasMoreElements())
				result.add(enumeration.nextElement());
			return result;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot enumerate classpath resources " + path, e);
		}
	}

	private static Properties readProperties(URL descriptor) {
		try (InputStream in = descriptor.openStream()) {
			Properties properties = new Properties();
			properties.load(in);
			return properties;
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read artifact descriptor " + descriptor, e);
		}
	}

	private static ArtifactReflection readReflection(URL descriptor, ClassLoader classLoader) {
		Properties properties = readProperties(descriptor);
		try {
			String className = required(properties, "reflection-class", descriptor);
			Class<?> type = Class.forName(className, true, classLoader);
			Field field = type.getField("reflection");
			ArtifactReflection reflection = (ArtifactReflection) field.get(null);
			verify(properties, "groupId", reflection.groupId(), descriptor);
			verify(properties, "artifactId", reflection.artifactId(), descriptor);
			verify(properties, "version", reflection.version(), descriptor);
			return reflection;
		} catch (ReflectiveOperationException | ClassCastException e) {
			throw new IllegalStateException("Cannot load artifact reflection declared by " + descriptor, e);
		}
	}

	private static String required(Properties properties, String key, URL descriptor) {
		String value = properties.getProperty(key);
		if (value == null || value.isBlank())
			throw new IllegalStateException("Missing " + key + " in artifact descriptor " + descriptor);
		return value;
	}

	private static void verify(Properties properties, String key, String value, URL descriptor) {
		if (!required(properties, key, descriptor).equals(value))
			throw new IllegalStateException("Artifact reflection differs from descriptor " + descriptor + " at " + key);
	}
}
