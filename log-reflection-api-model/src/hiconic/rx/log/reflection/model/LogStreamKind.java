package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum LogStreamKind implements EnumBase<LogStreamKind> {
	FILE,
	STRUCTURED_LIVE;

	public static final EnumType<LogStreamKind> T = EnumTypes.T(LogStreamKind.class);

	@Override
	public EnumType<LogStreamKind> type() {
		return T;
	}
}
