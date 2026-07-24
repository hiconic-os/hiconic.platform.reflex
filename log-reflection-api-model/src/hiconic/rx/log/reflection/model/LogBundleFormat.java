package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * Representation written into a log bundle.
 */
public enum LogBundleFormat implements EnumBase<LogBundleFormat> {
	/** Original active and, when requested, rotated files without normalization. */
	RAW_FILES,
	/** Canonical records as newline-delimited JSON. */
	CANONICAL_JSONL,
	/** Canonical records rendered as a human-readable log stream. */
	CANONICAL_TEXT;

	public static final EnumType<LogBundleFormat> T = EnumTypes.T(LogBundleFormat.class);

	@Override
	public EnumType<LogBundleFormat> type() {
		return T;
	}
}
