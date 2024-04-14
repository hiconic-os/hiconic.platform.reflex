// ============================================================================
package hiconic.rx.model.service.processing.md;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum InterceptionType implements EnumBase {
	preProcess,
	aroundProcess,
	postProcess;

	public static final EnumType T = EnumTypes.T(InterceptionType.class);
	
	@Override
	public EnumType type() {
		return T;
	}

}
