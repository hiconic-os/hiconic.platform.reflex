package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum LogParsingQuality implements EnumBase<LogParsingQuality> {
	EXACT,
	PARTIAL,
	RAW_ONLY;

	public static final EnumType<LogParsingQuality> T = EnumTypes.T(LogParsingQuality.class);

	@Override
	public EnumType<LogParsingQuality> type() {
		return T;
	}
}
