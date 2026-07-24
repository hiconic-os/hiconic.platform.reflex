// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.webapi.client.model.meta.params;

import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Maps Resource, List&lt;Resource&gt; or Set&lt;Resource&gt; to repeated binary multipart parts. */
public interface HttpMultipartResourcePart extends HttpMultipartPart {
	EntityType<HttpMultipartResourcePart> T = EntityTypes.T(HttpMultipartResourcePart.class);
}
