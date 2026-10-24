package hiconic.rx.webapi.client.api;

/** Signals that an HTTP response selected as a successful result could not be decoded according to its modeled body contract. */
public class HttpResponseDecodingException extends HttpClientException {
	private static final long serialVersionUID = 1L;

	private final int statusCode;
	private final HttpErrorResponse response;

	public HttpResponseDecodingException(int statusCode, HttpErrorResponse response, Throwable cause) {
		super(response.decodingFailure(), cause);
		this.statusCode = statusCode;
		this.response = response;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public HttpErrorResponse getResponse() {
		return response;
	}
}
