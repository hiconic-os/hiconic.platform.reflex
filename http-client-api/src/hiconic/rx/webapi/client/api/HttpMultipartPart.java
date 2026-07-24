// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// ============================================================================
package hiconic.rx.webapi.client.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpMultipartPart {
	private final String name;
	private final String fileName;
	private final String mimeType;
	private final Object content;
	private final HttpMultipartPartKind kind;
	private final Map<String, String> headers;

	/** Backwards-friendly convenience constructor for Resource parts. */
	public HttpMultipartPart(String name, String fileName, String mimeType, Object content) {
		this(name, fileName, mimeType, content, HttpMultipartPartKind.RESOURCE, Collections.emptyMap());
	}

	public HttpMultipartPart(String name, String fileName, String mimeType, Object content, HttpMultipartPartKind kind,
			Map<String, String> headers) {
		this.name = name;
		this.fileName = fileName;
		this.mimeType = mimeType;
		this.content = content;
		this.kind = kind;
		this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
	}

	public String getName() { return name; }
	public String getFileName() { return fileName; }
	public String getMimeType() { return mimeType; }
	public Object getContent() { return content; }
	public HttpMultipartPartKind getKind() { return kind; }
	public Map<String, String> getHeaders() { return headers; }
}
