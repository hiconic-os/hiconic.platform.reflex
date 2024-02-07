package hiconic.rx.demo.model.data;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

public enum Gender implements EnumBase {
	male, female;
	
	public static final EnumType T = EnumTypes.T(Gender.class);
	
	@Override
	public EnumType type() {
		return T;
	}
}
