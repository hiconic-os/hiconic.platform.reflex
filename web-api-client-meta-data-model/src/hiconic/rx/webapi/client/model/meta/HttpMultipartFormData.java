// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.webapi.client.model.meta;

import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.data.EntityTypeMetaData;

/** Maps a request to multipart/form-data. Explicit multipart property mappings are emitted as separate parts. */
public interface HttpMultipartFormData extends EntityTypeMetaData {

	EntityType<HttpMultipartFormData> T = EntityTypes.T(HttpMultipartFormData.class);

	/** Optional part containing the remaining marshalled request. Blank means that only explicit property parts are sent. */
	String getRequestPartName();
	void setRequestPartName(String requestPartName);

	@Initializer("'application/json'")
	String getRequestPartMimeType();
	void setRequestPartMimeType(String requestPartMimeType);
}
