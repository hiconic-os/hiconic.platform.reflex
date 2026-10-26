package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/** Selects the remote records from which a log download is built. */
public enum LogBundleScope implements EnumBase<LogBundleScope> {
	/** Only the active physical log file of every selected stream. */
	CURRENT,
	/** The active file and all discovered rotated files of every selected stream. */
	ALL,
	/** Records in the requested {@code from}/{@code to} interval across active and rotated files. */
	CUSTOM_RANGE;

	public static final EnumType<LogBundleScope> T = EnumTypes.T(LogBundleScope.class);

	@Override
	public EnumType<LogBundleScope> type() {
		return T;
	}
}
