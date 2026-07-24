// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.resource.model.packaged;

import com.braintribe.model.generic.base.EnumBase;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;

/** Logical indexed resource namespace in an RX application package. */
public enum PackagedResourceNamespace implements EnumBase<PackagedResourceNamespace> {
	resources,
	publicResources;

	public static final EnumType<PackagedResourceNamespace> T = EnumTypes.T(PackagedResourceNamespace.class);

	@Override
	public EnumType<PackagedResourceNamespace> type() {
		return T;
	}
}
