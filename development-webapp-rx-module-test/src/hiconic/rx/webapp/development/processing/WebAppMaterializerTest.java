package hiconic.rx.webapp.development.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

public class WebAppMaterializerTest {

	@Test
	public void materializesOnceAndReusesTheContentAddressedCache() throws Exception {
		Path root = Files.createTempDirectory("webapp-materializer-");
		Path archive = archive(root.resolve("explorer.war"), "index.html", "hello");
		AtomicInteger downloads = new AtomicInteger();
		ArtifactDownloader downloader = (contribution, downloadDirectory) -> {
			downloads.incrementAndGet();
			return archive;
		};
		WebAppMaterializer materializer = new WebAppMaterializer(root.resolve("cache"), downloader);
		WebAppContribution contribution = contribution();

		MaterializedWebApp first = materializer.materialize(List.of(contribution), false).getFirst();
		MaterializedWebApp second = materializer.materialize(List.of(contribution), false).getFirst();

		assertThat(downloads).hasValue(1);
		assertThat(Files.readString(first.contentDirectory().resolve("index.html"))).isEqualTo("hello");
		assertThat(second.contentDirectory()).isEqualTo(first.contentDirectory());

		materializer.materialize(List.of(contribution), true);
		assertThat(downloads).hasValue(2);
	}

	@Test
	public void rejectsArchiveEntriesEscapingTheCacheSlot() throws Exception {
		Path root = Files.createTempDirectory("webapp-materializer-slip-");
		Path archive = archive(root.resolve("bad.war"), "../outside.txt", "bad");
		WebAppMaterializer materializer = new WebAppMaterializer(root.resolve("cache"), (contribution, downloadDirectory) -> archive);

		assertThatThrownBy(() -> materializer.materialize(List.of(contribution()), false)) //
				.isInstanceOf(IllegalStateException.class) //
				.hasMessageContaining("escapes development web application target");
		assertThat(root.resolve("outside.txt")).doesNotExist();
	}

	private static WebAppContribution contribution() {
		return new WebAppContribution("example:explorer#[1,2)", "war", "/explorer", "index.html", "test:1");
	}

	private static Path archive(Path archive, String entryName, String content) throws Exception {
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
			zip.putNextEntry(new ZipEntry(entryName));
			zip.write(content.getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
		return archive;
	}
}
