package hiconic.rx.webapp.development.processing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WebAppMaterializer {
	private static final String COMPLETE_MARKER = ".complete";

	private final Path cacheRoot;
	private final ArtifactDownloader downloader;

	public WebAppMaterializer(Path cacheRoot, ArtifactDownloader downloader) {
		this.cacheRoot = cacheRoot.toAbsolutePath().normalize();
		this.downloader = downloader;
	}

	public List<MaterializedWebApp> materialize(List<WebAppContribution> contributions, boolean refresh) {
		try {
			Files.createDirectories(cacheRoot);
			try (FileChannel channel = FileChannel.open(cacheRoot.resolve(".materialization.lock"), StandardOpenOption.CREATE,
					StandardOpenOption.WRITE); FileLock ignored = channel.lock()) {
				List<MaterializedWebApp> result = new ArrayList<>(contributions.size());
				for (WebAppContribution contribution : contributions)
					result.add(materialize(contribution, refresh));
				return List.copyOf(result);
			}
		} catch (IOException e) {
			throw new UncheckedIOException("Could not materialize development web applications in " + cacheRoot, e);
		}
	}

	private MaterializedWebApp materialize(WebAppContribution contribution, boolean refresh) throws IOException {
		Path target = cacheRoot.resolve(hash(contribution.declaration()));
		Path content = target.resolve("content");
		if (!refresh && Files.isRegularFile(target.resolve(COMPLETE_MARKER)) && Files.isDirectory(content))
			return new MaterializedWebApp(contribution, content);

		deleteRecursively(target);
		Path staging = cacheRoot.resolve(target.getFileName() + ".tmp-" + UUID.randomUUID());
		try {
			Path archive = downloader.download(contribution, staging.resolve("download"));
			unzip(archive, staging.resolve("content"));
			writeManifest(staging, contribution, archive);
			Files.writeString(staging.resolve(COMPLETE_MARKER), "complete\n", StandardCharsets.UTF_8);
			move(staging, target);
		} finally {
			deleteRecursively(staging);
		}
		return new MaterializedWebApp(contribution, content);
	}

	private static void unzip(Path archive, Path target) throws IOException {
		Files.createDirectories(target);
		try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
			for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
				Path destination = target.resolve(entry.getName()).normalize();
				if (!destination.startsWith(target))
					throw new IllegalStateException("Archive entry escapes development web application target: " + entry.getName());
				if (entry.isDirectory())
					Files.createDirectories(destination);
				else {
					Files.createDirectories(destination.getParent());
					Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private static void writeManifest(Path staging, WebAppContribution contribution, Path archive) throws IOException {
		Properties properties = new Properties();
		properties.setProperty("declaration", contribution.declaration());
		properties.setProperty("source", contribution.source());
		properties.setProperty("archive", archive.getFileName().toString());
		try (var out = Files.newOutputStream(staging.resolve("materialization.properties"))) {
			properties.store(out, "Development web application cache");
		}
	}

	private static void move(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(source, target);
		}
	}

	private static void deleteRecursively(Path path) throws IOException {
		if (!Files.exists(path))
			return;
		try (var paths = Files.walk(path)) {
			for (Path entry : paths.sorted(Comparator.reverseOrder()).toList())
				Files.deleteIfExists(entry);
		}
	}

	private static String hash(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest, 0, 12);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError("Every Java runtime must provide SHA-256", e);
		}
	}
}
