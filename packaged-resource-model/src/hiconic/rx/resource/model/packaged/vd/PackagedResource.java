// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.resource.model.packaged.vd;

import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.value.ValueDescriptor;
import com.braintribe.model.resource.Resource;

/** Resolves an indexed immutable payload as a persistable {@link Resource}. */
@PositionalArguments("path")
public interface PackagedResource extends ValueDescriptor {

	EntityType<PackagedResource> T = EntityTypes.T(PackagedResource.class);

	String path = "path";
	String artifact = "artifact";

	String getPath();
	void setPath(String path);

	/** Owning artifact identity completed from the configuration source context. */
	String getArtifact();
	void setArtifact(String artifact);

	@Override
	default GenericModelType valueType() {
		return Resource.T;
	}
}
