package hiconic.rx.platform.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import hiconic.rx.platform.processing.resource.PackagedArtifact;

/**
 * Tests for {@link PackagedArtifact}.
 */
public class PackagedArtifactTest {

	@Test
	public void parsesPlainArtifactId() {
		PackagedArtifact artifact = PackagedArtifact.parse("example-configuration");

		assertThat(artifact.groupId()).isEmpty();
		assertThat(artifact.artifactId()).isEqualTo("example-configuration");
		assertThat(artifact.qualified()).isEqualTo("example-configuration");
	}

	@Test
	public void parsesCoordinate() {
		PackagedArtifact artifact = PackagedArtifact.parse("example.group:example-configuration");

		assertThat(artifact.groupId()).isEqualTo("example.group");
		assertThat(artifact.artifactId()).isEqualTo("example-configuration");
		assertThat(artifact.qualified()).isEqualTo("example.group:example-configuration");
	}

	@Test
	public void rejectsIncompleteCoordinates() {
		for (String invalid : new String[] { "", ":example-configuration", "example.group:", "a:b:c" })
			assertThatThrownBy(() -> PackagedArtifact.parse(invalid)).as(invalid).isInstanceOf(IllegalArgumentException.class);
	}

}
