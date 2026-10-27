package hiconic.rx.webapi.client.api;

import com.braintribe.gm.model.reason.Reason;

/**
 * Internal transport from the low-level HTTP client to the service processor. It preserves the observed error body
 * independently of whether decoding it according to the configured response mapping succeeded.
 */
public final class HttpErrorResponse {
	private final Object decodedBody;
	private final String textBody;
	private final String mimeType;
	private final String expectedBodyTypeSignature;
	private final long bodySize;
	private final boolean bodyTruncated;
	private final String decodingFailure;
	private final Reason decodingReason;

	/**
	 * @deprecated Prefer {@link #HttpErrorResponse(Object, String, String, String, long, boolean, String, Reason)} so a
	 *             marshaller-provided reason is not reduced to text.
	 */
	@Deprecated
	public HttpErrorResponse(Object decodedBody, String textBody, String mimeType, String expectedBodyTypeSignature,
			long bodySize, boolean bodyTruncated, String decodingFailure) {
		this(decodedBody, textBody, mimeType, expectedBodyTypeSignature, bodySize, bodyTruncated, decodingFailure, null);
	}

	public HttpErrorResponse(Object decodedBody, String textBody, String mimeType, String expectedBodyTypeSignature,
			long bodySize, boolean bodyTruncated, String decodingFailure, Reason decodingReason) {
		this.decodedBody = decodedBody;
		this.textBody = textBody;
		this.mimeType = mimeType;
		this.expectedBodyTypeSignature = expectedBodyTypeSignature;
		this.bodySize = bodySize;
		this.bodyTruncated = bodyTruncated;
		this.decodingFailure = decodingFailure;
		this.decodingReason = decodingReason;
	}

	public Object decodedBody() { return decodedBody; }
	public String textBody() { return textBody; }
	public String mimeType() { return mimeType; }
	public String expectedBodyTypeSignature() { return expectedBodyTypeSignature; }
	public long bodySize() { return bodySize; }
	public boolean bodyTruncated() { return bodyTruncated; }
	public String decodingFailure() { return decodingFailure; }
	public Reason decodingReason() { return decodingReason; }
}
