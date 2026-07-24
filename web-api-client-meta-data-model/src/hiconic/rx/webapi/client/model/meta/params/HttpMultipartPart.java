// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.webapi.client.model.meta.params;

import java.util.List;

import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/** Common mapping configuration for a request property represented by its own multipart part. */
@Abstract
public interface HttpMultipartPart extends HttpBodyParam {

	EntityType<HttpMultipartPart> T = EntityTypes.T(HttpMultipartPart.class);

	String getMimeType();
	void setMimeType(String mimeType);

	String getFileName();
	void setFileName(String fileName);

	/** Additional part headers in {@code Name: value} form. */
	List<String> getHeaders();
	void setHeaders(List<String> headers);
}
