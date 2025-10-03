package hiconic.rx.web.server.model.config;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum ExceptionExposure implements EnumBase<ExceptionExposure> {

	none,
	messageOnly,
	full;

	public static final EnumType<ExceptionExposure> T = EnumTypes.T(ExceptionExposure.class);

	@Override
	public EnumType<ExceptionExposure> type() {
		return T;
	}

}
