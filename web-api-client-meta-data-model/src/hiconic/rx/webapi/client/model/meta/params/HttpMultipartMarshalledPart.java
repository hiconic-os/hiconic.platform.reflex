// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.webapi.client.model.meta.params;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Maps a property as one independently marshalled multipart part. */
public interface HttpMultipartMarshalledPart extends HttpMultipartPart {
	EntityType<HttpMultipartMarshalledPart> T = EntityTypes.T(HttpMultipartMarshalledPart.class);

	@Override
	@Initializer("'application/json'")
	String getMimeType();
}
