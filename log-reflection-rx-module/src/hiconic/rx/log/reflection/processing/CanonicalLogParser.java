package hiconic.rx.log.reflection.processing;

import hiconic.rx.log.reflection.model.LogRecord;

/** Converts one physical log record into the framework-neutral representation. */
interface CanonicalLogParser {
	boolean startsRecord(String line);

	LogRecord parse(String raw, long byteOffset);

	LogRecord rawRecord(String raw, long byteOffset);
}
