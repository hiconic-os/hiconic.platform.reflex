// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.resource.model.packaged;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.annotation.meta.Mandatory;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.resource.source.ResourceSource;

/**
 * Stable reference to an immutable resource contributed to an indexed RX
 * application-package namespace.
 */
public interface PackagedResourceSource extends ResourceSource {

	EntityType<PackagedResourceSource> T = EntityTypes.T(PackagedResourceSource.class);

	String path = "path";
	String namespace = "namespace";
	String artifact = "artifact";

	@Mandatory
	String getPath();
	void setPath(String path);

	/**
	 * Optional artifact identity. If present, {@link #getPath()} is relative to that artifact's indexed-resource root.
	 * A missing artifact retains the original namespace-global addressing semantics.
	 */
	String getArtifact();
	void setArtifact(String artifact);

	@Initializer("enum(hiconic.rx.resource.model.packaged.PackagedResourceNamespace,resources)")
	PackagedResourceNamespace getNamespace();
	void setNamespace(PackagedResourceNamespace namespace);
}
