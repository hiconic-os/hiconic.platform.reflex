package hiconic.rx.platform.processing.resource;

import java.util.Objects;

import com.braintribe.model.resource.source.PackagedSource;

/**
 * The artifact of a packaged resource, as {@link PackagedSource#getArtifact()} names it: either {@code artifactId} or
 * {@code groupId:artifactId}.
 * <p>
 * The short form is the normal one. The groupId is only needed when two artifacts with the same artifactId contribute the same path, and an
 * artifact whose groupId is not known has an empty one.
 */
public record PackagedArtifact(String groupId, String artifactId) {

	public PackagedArtifact {
		Objects.requireNonNull(groupId, "groupId");
		if (artifactId == null || artifactId.isBlank())
			throw new IllegalArgumentException("A packaged resource artifact must not be empty");
	}

	/** Parses {@code artifactId} or {@code groupId:artifactId}. */
	public static PackagedArtifact parse(String artifact) {
		if (artifact == null || artifact.isBlank())
			throw new IllegalArgumentException("A packaged resource artifact must not be empty");

		int separator = artifact.indexOf(':');
		if (separator < 0)
			return new PackagedArtifact("", artifact);

		if (separator == 0 || separator == artifact.length() - 1 || artifact.indexOf(':', separator + 1) >= 0)
			throw new IllegalArgumentException("Invalid packaged resource artifact, expected artifactId or groupId:artifactId: " + artifact);

		return new PackagedArtifact(artifact.substring(0, separator), artifact.substring(separator + 1));
	}

	public boolean hasGroupId() {
		return !groupId.isEmpty();
	}

	/** {@code groupId:artifactId}, or just {@code artifactId} if the groupId is not known. */
	public String qualified() {
		return hasGroupId() ? groupId + ":" + artifactId : artifactId;
	}

	@Override
	public String toString() {
		return qualified();
	}

}
