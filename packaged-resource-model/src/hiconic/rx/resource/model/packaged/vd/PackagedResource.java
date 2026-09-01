// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.resource.model.packaged.vd;

import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Resolves an indexed immutable payload as a persistable {@link Resource}. */
@PositionalArguments({ "path", "artifact" })
public interface PackagedResource extends com.braintribe.model.resource.source.vd.ArtifactResource {

	EntityType<PackagedResource> T = EntityTypes.T(PackagedResource.class);

}
