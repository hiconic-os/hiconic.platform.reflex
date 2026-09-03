package hiconic.rx.webapp.development.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class WebAppContributionReaderTest {

	@Test
	public void readsAdditiveContributionsFromTheActiveClasspath() throws Exception {
		Path first = contribution("example:explorer#[1,2)/war=/explorer;welcome=index.html\n");
		Path second = contribution("example:logs#[1,2)/web-app:zip=log-reflection/\n");

		try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[] { first.toUri().toURL(), second.toUri().toURL() }, null)) {
			assertThat(new WebAppContributionReader().read(classLoader)) //
					.extracting(WebAppContribution::serverPath) //
					.containsExactly("/explorer", "/log-reflection");
		}
	}

	@Test
	public void rejectsConflictingContributionsForOneServerPath() throws Exception {
		Path first = contribution("example:explorer#[1,2)/war=/explorer\n");
		Path second = contribution("example:other#[1,2)/war=/explorer\n");

		try (URLClassLoader classLoader = new URLClassLoader(new java.net.URL[] { first.toUri().toURL(), second.toUri().toURL() }, null)) {
			assertThatThrownBy(() -> new WebAppContributionReader().read(classLoader)) //
					.isInstanceOf(IllegalStateException.class) //
					.hasMessageContaining("Conflicting development web applications");
		}
	}

	private static Path contribution(String content) throws Exception {
		Path root = Files.createTempDirectory("webapp-contribution-");
		Path descriptor = root.resolve(WebAppContributionReader.CONTRIBUTION_PATH);
		Files.createDirectories(descriptor.getParent());
		Files.writeString(descriptor, content);
		return root;
	}
}
