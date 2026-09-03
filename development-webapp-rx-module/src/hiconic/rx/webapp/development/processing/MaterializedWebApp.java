package hiconic.rx.webapp.development.processing;

import java.nio.file.Path;

public record MaterializedWebApp(WebAppContribution contribution, Path contentDirectory) {
}
