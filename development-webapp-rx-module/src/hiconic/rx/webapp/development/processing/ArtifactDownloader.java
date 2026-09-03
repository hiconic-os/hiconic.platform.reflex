package hiconic.rx.webapp.development.processing;

import java.nio.file.Path;

@FunctionalInterface
public interface ArtifactDownloader {
	Path download(WebAppContribution contribution, Path downloadDirectory);
}
