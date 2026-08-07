// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.loading;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import com.braintribe.common.artifact.ArtifactReflection;

import hiconic.rx.module.api.wire.RxModule;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Resolves the generated artifact reflection owning an RX module without requiring module-specific declarations. */
final class ModuleArtifactReflectionResolver {

	private static final String DESCRIPTOR_PATH = "META-INF/artifact-descriptor.properties";

	static ArtifactReflection resolve(RxModule<?> module) {
		Class<?> moduleClass = module.getClass();
		URL location = codeSourceLocation(moduleClass);

		List<DescriptorSource> candidates = descriptorCandidates(location);
		List<ArtifactReflection> reflections = new ArrayList<>();
		List<String> failures = new ArrayList<>();

		for (DescriptorSource candidate : candidates) {
			try {
				ArtifactReflection reflection = loadReflection(candidate, moduleClass.getClassLoader());
				if (reflection != null)
					reflections.add(reflection);
			} catch (Exception e) {
				failures.add(candidate.description() + ": " + e.getMessage());
			}
		}

		if (reflections.size() == 1)
			return reflections.get(0);

		if (reflections.isEmpty()) {
			ArtifactReflection pomReflection = reflectionFromPomNearLocation(location);
			if (pomReflection != null)
				return pomReflection;

			ArtifactReflection legacyReflection = reflectionFromLegacyJarName(location);
			if (legacyReflection != null)
				return legacyReflection;

			throw new IllegalStateException("Cannot resolve artifact reflection for RX module " + moduleClass.getName() + " loaded from " + location
					+ ". Checked: " + candidates.stream().map(DescriptorSource::description).toList()
					+ (failures.isEmpty() ? "" : ". Invalid descriptors: " + failures));
		}

		throw new IllegalStateException("Ambiguous artifact reflection for RX module " + moduleClass.getName() + " loaded from " + location + ": "
				+ reflections.stream().map(ArtifactReflection::versionedName).toList());
	}

	private static URL codeSourceLocation(Class<?> moduleClass) {
		if (moduleClass.getProtectionDomain() == null || moduleClass.getProtectionDomain().getCodeSource() == null
				|| moduleClass.getProtectionDomain().getCodeSource().getLocation() == null)
			throw new IllegalStateException("RX module class has no code source: " + moduleClass.getName());

		return moduleClass.getProtectionDomain().getCodeSource().getLocation();
	}

	private static List<DescriptorSource> descriptorCandidates(URL location) {
		if (!"file".equalsIgnoreCase(location.getProtocol()))
			throw new IllegalStateException("Unsupported RX module code source URL: " + location);

		Path path;
		try {
			path = Path.of(new URI(location.toExternalForm())).toAbsolutePath().normalize();
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid RX module code source URL: " + location, e);
		}

		if (Files.isRegularFile(path))
			return List.of(DescriptorSource.jar(path));

		Set<Path> paths = new LinkedHashSet<>();
		paths.add(path.resolve(DESCRIPTOR_PATH));

		Path artifactRoot = findArtifactRoot(path);
		if (artifactRoot != null) {
			paths.add(artifactRoot.resolve("class-gen").resolve(DESCRIPTOR_PATH));
			paths.add(artifactRoot.resolve("build").resolve(DESCRIPTOR_PATH));
			paths.add(artifactRoot.resolve("target/classes").resolve(DESCRIPTOR_PATH));
		}

		return paths.stream().filter(Files::isRegularFile).map(DescriptorSource::file).toList();
	}

	private static Path findArtifactRoot(Path start) {
		for (Path current = start; current != null; current = current.getParent())
			if (Files.isRegularFile(current.resolve("pom.xml")))
				return current;

		return null;
	}

	private static ArtifactReflection reflectionFromPomNearLocation(URL location) {
		if (!"file".equalsIgnoreCase(location.getProtocol()))
			return null;

		try {
			Path codeSource = Path.of(location.toURI()).toAbsolutePath().normalize();
			Path pom;
			if (Files.isDirectory(codeSource)) {
				Path artifactRoot = findArtifactRoot(codeSource);
				pom = artifactRoot == null ? null : artifactRoot.resolve("pom.xml");
			} else {
				String fileName = codeSource.getFileName().toString();
				pom = fileName.endsWith(".jar") ? codeSource.resolveSibling(fileName.substring(0, fileName.length() - 4) + ".pom") : null;
			}
			if (pom == null || !Files.isRegularFile(pom))
				return null;

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

			Element project = factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement();
			Element parent = directChild(project, "parent");
			String groupId = firstNonBlank(directText(project, "groupId"), directText(parent, "groupId"));
			String artifactId = directText(project, "artifactId");
			String version = firstNonBlank(directText(project, "version"), directText(parent, "version"));
			if (groupId == null || artifactId == null || version == null)
				throw new IllegalStateException("Incomplete artifact coordinates in " + pom);

			return new SimpleArtifactReflection(groupId, artifactId, version);
		} catch (Exception e) {
			throw new IllegalStateException("Cannot derive artifact reflection from POM near RX module location " + location, e);
		}
	}

	/** Compatibility for already published RX modules predating generated artifact descriptors. */
	private static ArtifactReflection reflectionFromLegacyJarName(URL location) {
		if (!"file".equalsIgnoreCase(location.getProtocol()))
			return null;

		try {
			return reflectionFromLegacyJarName(Path.of(location.toURI()).toAbsolutePath().normalize());
		} catch (Exception e) {
			throw new IllegalStateException("Cannot derive legacy artifact reflection from RX module location " + location, e);
		}
	}

	static ArtifactReflection reflectionFromLegacyJarName(Path path) {
		try {
			if (!Files.isRegularFile(path))
				return null;

			String fileName = path.getFileName().toString();
			if (!fileName.endsWith(".jar"))
				return null;

			String stem = fileName.substring(0, fileName.length() - 4);
			for (int separator = stem.indexOf('-'); separator >= 0; separator = stem.indexOf('-', separator + 1)) {
				String version = stem.substring(separator + 1);
				if (version.matches("\\d+\\.\\d+\\.\\d+(?:[-.][A-Za-z0-9_.-]+)?")) {
					String artifactId = stem.substring(0, separator);
					String groupId = groupIdFromPackagedSolutions(path, artifactId, version);
					return new SimpleArtifactReflection(groupId == null ? "legacy.unresolved" : groupId, artifactId, version);
				}
			}
			return null;
		} catch (Exception e) {
			throw new IllegalStateException("Cannot derive legacy artifact reflection from RX module location " + path, e);
		}
	}

	private static String groupIdFromPackagedSolutions(Path jar, String artifactId, String version) throws IOException {
		Path lib = jar.getParent();
		Path application = lib == null ? null : lib.getParent();
		Path solutions = application == null ? null : application.resolve("packaged-solutions.txt");
		if (solutions == null || !Files.isRegularFile(solutions))
			return null;

		String suffix = ":" + artifactId + "#" + version;
		List<String> matches = Files.readAllLines(solutions).stream().filter(line -> line.endsWith(suffix)).toList();
		if (matches.size() > 1)
			throw new IllegalStateException("Ambiguous artifact coordinates for " + jar + " in " + solutions + ": " + matches);
		return matches.isEmpty() ? null : matches.get(0).substring(0, matches.get(0).length() - suffix.length());
	}

	private static Element directChild(Element parent, String name) {
		if (parent == null)
			return null;
		for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
			if (child instanceof Element element && name.equals(element.getLocalName() == null ? element.getNodeName() : element.getLocalName()))
				return element;
		return null;
	}

	private static String directText(Element parent, String name) {
		Element child = directChild(parent, name);
		if (child == null)
			return null;
		String value = child.getTextContent();
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static String firstNonBlank(String first, String second) {
		return first != null ? first : second;
	}

	private record SimpleArtifactReflection(String groupId, String artifactId, String version) implements ArtifactReflection {
		@Override public Set<String> archetypes() { return Set.of(); }
		@Override public String name() { return groupId + ":" + artifactId; }
		@Override public String versionedName() { return name() + "#" + version; }
	}

	private static ArtifactReflection loadReflection(DescriptorSource source, ClassLoader classLoader) throws Exception {
		Properties properties = new Properties();
		try (InputStream in = source.open()) {
			if (in == null)
				return null;
			properties.load(in);
		}

		String reflectionClassName = required(properties, "reflection-class", source);
		Class<?> reflectionClass = Class.forName(reflectionClassName, true, classLoader);
		Field reflectionField = reflectionClass.getField("reflection");
		Object value = reflectionField.get(null);
		if (!(value instanceof ArtifactReflection reflection))
			throw new IllegalStateException("Field 'reflection' of " + reflectionClassName + " is not an ArtifactReflection");

		verify(properties, "groupId", reflection.groupId(), source);
		verify(properties, "artifactId", reflection.artifactId(), source);
		verify(properties, "version", reflection.version(), source);
		return reflection;
	}

	private static String required(Properties properties, String name, DescriptorSource source) {
		String value = properties.getProperty(name);
		if (value == null || value.isBlank())
			throw new IllegalStateException("Missing property '" + name + "' in " + source.description());
		return value;
	}

	private static void verify(Properties properties, String name, String actual, DescriptorSource source) {
		String expected = required(properties, name, source);
		if (!expected.equals(actual))
			throw new IllegalStateException("Artifact reflection property '" + name + "' is '" + actual + "' but descriptor " + source.description()
					+ " declares '" + expected + "'");
	}

	private interface DescriptorSource {
		InputStream open() throws IOException;
		String description();

		static DescriptorSource file(Path descriptor) {
			return new DescriptorSource() {
				@Override public InputStream open() throws IOException { return Files.newInputStream(descriptor); }
				@Override public String description() { return descriptor.toString(); }
			};
		}

		static DescriptorSource jar(Path jar) {
			return new DescriptorSource() {
				private JarFile openedJar;

				@Override
				public InputStream open() throws IOException {
					openedJar = new JarFile(jar.toFile());
					JarEntry entry = openedJar.getJarEntry(DESCRIPTOR_PATH);
					if (entry == null) {
						openedJar.close();
						openedJar = null;
						return null;
					}
					InputStream input = openedJar.getInputStream(entry);
					return new InputStream() {
						@Override public int read() throws IOException { return input.read(); }
						@Override public int read(byte[] b, int off, int len) throws IOException { return input.read(b, off, len); }
						@Override public void close() throws IOException { try { input.close(); } finally { openedJar.close(); } }
					};
				}

				@Override public String description() { return jar + "!/" + DESCRIPTOR_PATH; }
			};
		}
	}

}
