package hiconic.rx.platform.conf;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.braintribe.gm.config.yaml.index.ClasspathEntry;
import com.braintribe.gm.config.yaml.index.ClasspathIndex;

public class LayeredConfigurationEntries {
	private final ClasspathIndex classpathIndex;
	private final String classpathConfPath;
	private final File configFolder;
	private final String baseName;
	private final String extension;

	public LayeredConfigurationEntries(ClasspathIndex classpathIndex, String classpathConfPath, File configFolder, String baseName, String extension) {
		this.classpathIndex = Objects.requireNonNull(classpathIndex, "classpathIndex");
		this.classpathConfPath = normalizeClasspathConfPath(classpathConfPath);
		this.configFolder = Objects.requireNonNull(configFolder, "configFolder");
		this.baseName = Objects.requireNonNull(baseName, "baseName");
		this.extension = normalizeExtension(extension);
	}

	public List<Entry> entries() {
		List<Entry> classpathEntries = classpathEntries();
		List<Entry> fileEntries = fileEntries();

		List<Entry> entries = new ArrayList<>(classpathEntries.size() + fileEntries.size());
		entries.addAll(classpathEntries);
		entries.addAll(fileEntries);
		return entries;
	}

	private List<Entry> classpathEntries() {
		String resourcePrefix = classpathConfPath + baseName;
		List<Entry> entries = new ArrayList<>();

		for (ClasspathEntry classpathEntry : classpathIndex.forPrefix(resourcePrefix)) {
			Optional<Variant> variant = variant(classpathEntry.path);
			if (variant.isEmpty())
				continue;

			entries.add(Entry.fromClasspath(classpathEntry, variant.get()));
		}

		entries.sort(Entry::compareTo);
		return entries;
	}

	private List<Entry> fileEntries() {
		if (!configFolder.exists())
			return Collections.emptyList();

		File[] files = configFolder.listFiles();
		if (files == null)
			return Collections.emptyList();

		List<Entry> entries = new ArrayList<>();

		for (File file : files) {
			Optional<Variant> variant = variant(file.getName());
			if (variant.isEmpty())
				continue;

			entries.add(Entry.fromFile(file, variant.get()));
		}

		entries.sort(Entry::compareTo);
		return entries;
	}

	private Optional<Variant> variant(String pathOrName) {
		String name = fileName(pathOrName);
		String baseFileName = baseName + extension;

		if (name.equals(baseFileName))
			return Optional.of(new Variant("", -1));

		String prefix = baseName + ".";
		if (!name.startsWith(prefix) || !name.endsWith(extension))
			return Optional.empty();

		String disambiguator = name.substring(prefix.length(), name.length() - extension.length());
		int priority = priority(disambiguator);
		if (priority != -1)
			disambiguator = stripPriority(disambiguator);

		return Optional.of(new Variant(disambiguator, priority));
	}

	private int priority(String disambiguator) {
		if (disambiguator.isEmpty())
			return -1;

		int dash = disambiguator.lastIndexOf('-');
		String candidate = dash == -1 ? disambiguator : disambiguator.substring(dash + 1);

		try {
			return Integer.parseInt(candidate);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private String stripPriority(String disambiguator) {
		int dash = disambiguator.lastIndexOf('-');
		return dash == -1 ? "" : disambiguator.substring(0, dash);
	}

	private String fileName(String pathOrName) {
		int slash = pathOrName.lastIndexOf('/');
		return slash == -1 ? pathOrName : pathOrName.substring(slash + 1);
	}

	private static String normalizeClasspathConfPath(String classpathConfPath) {
		String normalized = Objects.requireNonNull(classpathConfPath, "classpathConfPath").replace('\\', '/');
		while (normalized.startsWith("/"))
			normalized = normalized.substring(1);
		return normalized.endsWith("/") ? normalized : normalized + "/";
	}

	private static String normalizeExtension(String extension) {
		Objects.requireNonNull(extension, "extension");
		return extension.startsWith(".") ? extension : "." + extension;
	}

	private static class Variant {
		private final String disambiguator;
		private final int priority;

		private Variant(String disambiguator, int priority) {
			this.disambiguator = disambiguator;
			this.priority = priority;
		}
	}

	public static class Entry implements Comparable<Entry> {
		private static final Comparator<Entry> comparator = Comparator.comparingInt((Entry e) -> e.priority) //
				.thenComparing(e -> e.disambiguator) //
				.thenComparing(e -> e.artifactId) //
				.thenComparing(e -> e.source);

		private final URL url;
		private final File file;
		private final String source;
		private final String disambiguator;
		private final int priority;
		private final String artifactId;

		private Entry(URL url, File file, String source, String disambiguator, int priority, String artifactId) {
			this.url = url;
			this.file = file;
			this.source = source;
			this.disambiguator = disambiguator;
			this.priority = priority;
			this.artifactId = artifactId;
		}

		private static Entry fromClasspath(ClasspathEntry classpathEntry, Variant variant) {
			return new Entry(classpathEntry.url, null, classpathEntry.path, variant.disambiguator, variant.priority, artifactId(classpathEntry.url));
		}

		private static Entry fromFile(File file, Variant variant) {
			return new Entry(null, file, file.getAbsolutePath(), variant.disambiguator, variant.priority, "");
		}

		public URL url() {
			return url;
		}

		public File file() {
			return file;
		}

		public String source() {
			return source;
		}

		public boolean classpath() {
			return url != null;
		}

		@Override
		public int compareTo(Entry other) {
			return comparator.compare(this, other);
		}

		private static String artifactId(URL url) {
			String path = url.toString();
			int jarEnd = path.indexOf(".jar!");
			if (jarEnd == -1)
				return "";

			int nameStart = path.lastIndexOf('/', jarEnd);
			String jarName = path.substring(nameStart + 1, jarEnd);
			int versionDash = jarName.lastIndexOf('-');
			return versionDash == -1 ? jarName : jarName.substring(0, versionDash);
		}
	}
}
