package hiconic.rx.log.reflection.processing;

import java.nio.file.Path;
import java.util.List;

import hiconic.rx.log.reflection.model.LogStreamDescriptor;

record LogbackFileStream(Path path, List<Path> segmentPaths, LogStreamDescriptor descriptor, CanonicalLogParser parser) {
}
