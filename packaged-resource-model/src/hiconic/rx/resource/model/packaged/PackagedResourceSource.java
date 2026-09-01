// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.resource.model.packaged;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * Stable reference to an immutable resource contributed to an indexed RX
 * application-package namespace.
 */
public interface PackagedResourceSource extends com.braintribe.model.resource.source.ArtifactResourceSource {

	EntityType<PackagedResourceSource> T = EntityTypes.T(PackagedResourceSource.class);

	String namespace = "namespace";

	@Initializer("enum(hiconic.rx.resource.model.packaged.PackagedResourceNamespace,resources)")
	PackagedResourceNamespace getNamespace();
	void setNamespace(PackagedResourceNamespace namespace);
}
