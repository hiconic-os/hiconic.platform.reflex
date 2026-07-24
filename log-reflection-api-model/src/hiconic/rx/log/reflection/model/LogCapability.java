package hiconic.rx.log.reflection.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum LogCapability implements EnumBase<LogCapability> {
	TIMESTAMP,
	LEVEL,
	LOGGER,
	THREAD,
	SOURCE_LOCATION,
	THROWABLE,
	CUSTOM_PROPERTIES,
	FULLTEXT;

	public static final EnumType<LogCapability> T = EnumTypes.T(LogCapability.class);

	@Override
	public EnumType<LogCapability> type() {
		return T;
	}
}
