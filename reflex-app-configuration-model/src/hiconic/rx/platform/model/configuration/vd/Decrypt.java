// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.platform.model.configuration.vd;

import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.generic.value.type.StringDescriptor;

/** Decrypts a configuration value with the platform's configured decryption secret. */
@PositionalArguments("value")
public interface Decrypt extends StringDescriptor {

	EntityType<Decrypt> T = EntityTypes.T(Decrypt.class);

	String getValue();
	void setValue(String value);
}
