package hiconic.rx.setup.model;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/**
 * 
 * 
 * @author peter.gazdik
 */
public enum RxPlatform implements EnumBase {
	web,
	cli;

	public static final EnumType T = EnumTypes.T(RxPlatform.class);

	@Override
	public EnumType type() {
		return T;
	}

}
