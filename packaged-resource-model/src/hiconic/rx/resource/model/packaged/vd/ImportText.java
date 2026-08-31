// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.resource.model.packaged.vd;

import com.braintribe.model.generic.annotation.meta.PositionalArguments;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.generic.value.type.StringDescriptor;

/** Imports UTF-8 text from an indexed resource, normally relative to the owning modeled configuration resource. */
@PositionalArguments({ "path", "artifact" })
public interface ImportText extends StringDescriptor {

	EntityType<ImportText> T = EntityTypes.T(ImportText.class);

	String path = "path";
	String artifact = "artifact";

	String getPath();
	void setPath(String path);

	/** Owning artifact identity completed from the configuration source context. */
	String getArtifact();
	void setArtifact(String artifact);
}
