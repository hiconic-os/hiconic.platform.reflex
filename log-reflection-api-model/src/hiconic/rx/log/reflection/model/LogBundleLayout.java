package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/** Determines whether selected records retain their physical segment boundaries. */
public enum LogBundleLayout implements EnumBase<LogBundleLayout> {
	/** One result per selected node and logical stream. Physical rotations are folded into that result. */
	SEPARATE,
	/** One result per logical stream across all selected nodes. */
	MERGED;

	public static final EnumType<LogBundleLayout> T = EnumTypes.T(LogBundleLayout.class);

	@Override
	public EnumType<LogBundleLayout> type() {
		return T;
	}
}
