// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.webapi.client.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable multipart/form-data description attached to an HttpRequestContext. */
public class HttpMultipartFormData {
	private final String requestPartName;
	private final String requestPartMimeType;
	private final List<HttpMultipartPart> parts;

	public HttpMultipartFormData(String requestPartName, String requestPartMimeType, List<HttpMultipartPart> parts) {
		this.requestPartName = requestPartName;
		this.requestPartMimeType = requestPartMimeType;
		this.parts = Collections.unmodifiableList(new ArrayList<>(parts));
	}

	public String getRequestPartName() { return requestPartName; }
	public String getRequestPartMimeType() { return requestPartMimeType; }
	public List<HttpMultipartPart> getParts() { return parts; }
}
