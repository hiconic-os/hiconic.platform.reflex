package hiconic.rx.webapp.development.processing;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class JinniArtifactDownloader implements ArtifactDownloader {

	@Override
	public Path download(WebAppContribution contribution, Path downloadDirectory) {
		try {
			Files.createDirectories(downloadDirectory);
			List<String> command = command(contribution, downloadDirectory);
			Process process = new ProcessBuilder(command) //
					.directory(new File(System.getProperty("user.dir"))) //
					.redirectErrorStream(true) //
					.start();
			String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			int exitCode = process.waitFor();
			if (exitCode != 0)
				throw new IllegalStateException("jinni download-artifacts failed with exit code " + exitCode + " for "
						+ contribution.dependency() + "/" + contribution.part() + ":\n" + tail(output, 8_000));

			return findDownloadedArchive(contribution, downloadDirectory);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while downloading " + contribution.dependency(), e);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not run jinni to download " + contribution.dependency(), e);
		}
	}

	private List<String> command(WebAppContribution contribution, Path downloadDirectory) {
		List<String> arguments = List.of( //
				"download-artifacts", //
				"--path", downloadDirectory.toAbsolutePath().toString(), //
				"--artifacts", contribution.dependency(), //
				"--mandatoryParts", contribution.part(), //
				"--transitive", "false");

		if (!isWindows()) {
			List<String> command = new ArrayList<>();
			command.add(jinniLauncher("jinni"));
			command.addAll(arguments);
			return command;
		}

		List<String> command = new ArrayList<>();
		command.add("cmd.exe");
		command.add("/d");
		command.add("/c");
		command.add("call");
		command.add(jinniLauncher("jinni.bat"));
		command.addAll(arguments);
		return command;
	}

	private String jinniLauncher(String fileName) {
		String sdkHome = System.getenv("HICONIC_SDK_HOME");
		if (sdkHome == null || sdkHome.isBlank())
			return fileName;
		return Path.of(sdkHome).resolve("tools/jinni/bin").resolve(fileName).toString();
	}

	private Path findDownloadedArchive(WebAppContribution contribution, Path downloadDirectory) throws IOException {
		String artifactId = artifactId(contribution.dependency());
		String suffix = archiveSuffix(contribution.part());
		try (var paths = Files.walk(downloadDirectory)) {
			List<Path> matches = paths //
					.filter(Files::isRegularFile) //
					.filter(path -> {
						String name = path.getFileName().toString();
						return name.startsWith(artifactId + "-") && name.endsWith(suffix);
					}) //
					.sorted(Comparator.comparing(Path::toString)) //
					.toList();
			if (matches.size() != 1)
				throw new IllegalStateException("Expected exactly one downloaded " + contribution.part() + " part for "
						+ contribution.dependency() + " but found " + matches);
			return matches.get(0);
		}
	}

	private static String artifactId(String dependency) {
		int colon = dependency.indexOf(':');
		int hash = dependency.indexOf('#', colon + 1);
		return dependency.substring(colon + 1, hash);
	}

	private static String archiveSuffix(String part) {
		int colon = part.indexOf(':');
		return colon < 0 ? "." + part : "-" + part.substring(0, colon) + "." + part.substring(colon + 1);
	}

	private static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}

	private static String tail(String text, int maximumLength) {
		return text.length() <= maximumLength ? text : text.substring(text.length() - maximumLength);
	}
}
