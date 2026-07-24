package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum LogFormat implements EnumBase<LogFormat> {
	STRUCTURED_EVENT,
	JSON,
	PATTERN,
	RAW;

	public static final EnumType<LogFormat> T = EnumTypes.T(LogFormat.class);

	@Override
	public EnumType<LogFormat> type() {
		return T;
	}
}
