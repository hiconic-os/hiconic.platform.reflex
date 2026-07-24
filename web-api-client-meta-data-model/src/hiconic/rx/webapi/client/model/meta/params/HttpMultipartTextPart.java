// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.webapi.client.model.meta.params;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Maps a scalar, or every element of a scalar collection, to textual multipart parts. */
public interface HttpMultipartTextPart extends HttpMultipartPart {
	EntityType<HttpMultipartTextPart> T = EntityTypes.T(HttpMultipartTextPart.class);

	@Override
	@Initializer("'text/plain; charset=UTF-8'")
	String getMimeType();
}
