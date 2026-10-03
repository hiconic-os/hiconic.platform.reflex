package hiconic.rx.platform.conf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.braintribe.gm.config.yaml.index.ClasspathIndex;

import hiconic.rx.platform.conf.LayeredConfigurationEntries.Entry;

/**
 * Tests for {@link LayeredConfigurationEntries}.
 * <p>
 * The entries come from an assembled filesystem mirror, whose URLs are plain files, just like those of a project in the IDE. Their artifact is
 * therefore known only from the {@link ClasspathIndex}, not from the URL.
 */
public class LayeredConfigurationEntriesTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	/** Equal priority and disambiguator, so the artifactId decides, not the order in which the index lists the artifacts. */
	@Test
	public void sortsSamePriorityByArtifactId() throws Exception {
		Path mirror = temporaryFolder.newFolder("packaged-resources").toPath();
		writeMirror(mirror, //
				new MirrorArtifact("first", "", "zzz-configuration"), //
				new MirrorArtifact("second", "", "aaa-configuration"));

		assertThat(foldersOf(entries(mirror))).containsExactly("second", "first");
	}

	@Test
	public void sortsSameArtifactIdByGroupId() throws Exception {
		Path mirror = temporaryFolder.newFolder("packaged-resources").toPath();
		writeMirror(mirror, //
				new MirrorArtifact("first", "zzz.group", "configuration"), //
				new MirrorArtifact("second", "aaa.group", "configuration"));

		assertThat(foldersOf(entries(mirror))).containsExactly("second", "first");
	}

	/** The groupId is not known for every artifact, so it must not decide between different artifactIds. */
	@Test
	public void sortsByArtifactIdBeforeGroupId() throws Exception {
		Path mirror = temporaryFolder.newFolder("packaged-resources").toPath();
		writeMirror(mirror, //
				new MirrorArtifact("first", "", "zzz-configuration"), //
				new MirrorArtifact("second", "zzz.group", "aaa-configuration"));

		assertThat(foldersOf(entries(mirror))).containsExactly("second", "first");
	}

	private List<Entry> entries(Path mirror) {
		File noConfFolder = new File(temporaryFolder.getRoot(), "no-conf");
		return new LayeredConfigurationEntries(new ClasspathIndex(mirror), "HICONIC-CONF", noConfFolder, "properties", ".yaml").entries();
	}

	/** The mirror folder of each entry, which tells the artifact it came from. */
	private static List<String> foldersOf(List<Entry> entries) {
		return entries.stream().map(entry -> {
			try {
				return Path.of(entry.url().toURI()).getParent().getParent().getFileName().toString();
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}).toList();
	}

	private record MirrorArtifact(String folder, String groupId, String artifactId) {
	}

	/** Writes a mirror in which every artifact contributes a plain HICONIC-CONF/properties.yaml, listed in the given order. */
	private static void writeMirror(Path mirror, MirrorArtifact... artifacts) throws Exception {
		StringBuilder index = new StringBuilder("formatVersion=1\nartifact.count=" + artifacts.length + "\n");

		for (int i = 0; i < artifacts.length; i++) {
			MirrorArtifact artifact = artifacts[i];
			String prefix = "artifact." + i + ".";

			index.append(prefix).append("folder=").append(artifact.folder()).append('\n');
			if (!artifact.groupId().isEmpty())
				index.append(prefix).append("groupId=").append(artifact.groupId()).append('\n');
			index.append(prefix).append("artifactId=").append(artifact.artifactId()).append('\n');
			index.append(prefix).append("resource.count=1\n");
			index.append(prefix).append("resource.0.path=HICONIC-CONF/properties.yaml\n");

			Path properties = mirror.resolve(artifact.folder()).resolve("HICONIC-CONF/properties.yaml");
			Files.createDirectories(properties.getParent());
			Files.writeString(properties, "origin: " + artifact.folder() + "\n", StandardCharsets.UTF_8);
		}

		Files.writeString(mirror.resolve("index.properties"), index, StandardCharsets.UTF_8);
	}

}
