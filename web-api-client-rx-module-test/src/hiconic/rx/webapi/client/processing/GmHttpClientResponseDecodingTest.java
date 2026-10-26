package hiconic.rx.webapi.client.processing;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.MarshallException;
import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.common.BasicConfigurableMarshallerRegistry;
import com.braintribe.exception.HttpException;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.transport.http.ClientParameters;
import com.braintribe.transport.http.HttpClientProvider;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.stream.api.StreamPipes;

import hiconic.rx.webapi.client.api.HttpErrorResponse;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpRequestContextBuilder;
import hiconic.rx.webapi.client.api.HttpResponse;
import hiconic.rx.webapi.client.api.HttpResponseDecodingException;
import hiconic.rx.webapi.client.api.HttpResponseMapping;

public class GmHttpClientResponseDecodingTest {

	private static final String MIME_TYPE = "application/x-response-decoding-test";

	@Test
	public void negativeResponseKeepsFullBodyWhenFailingMarshallerClosesItsInput() {
		String body = "{\"unexpected\":\"negative response shape\"}";
		GmHttpClient client = client(400, body, ClosingMarshaller.failingAfter(1));

		HttpException failure = assertThrows(HttpException.class, () -> client.sendRequest(negativeContext(client)));

		HttpErrorResponse response = (HttpErrorResponse) failure.getPayload();
		assertEquals(body, response.textBody());
		assertEquals(body.getBytes(UTF_8).length, response.bodySize());
		assertFalse(response.bodyTruncated());
		assertTrue(response.decodingFailure().contains("deliberate decoding failure"));
		assertFalse(response.decodingFailure().contains("closed stream"));
	}

	@Test
	public void positiveResponseKeepsBodyWhenFailingMarshallerClosesItsInput() {
		String body = "{\"unexpected\":\"positive response shape\"}";
		GmHttpClient client = client(200, body, ClosingMarshaller.failingAfter(2));

		HttpResponseDecodingException failure = assertThrows(HttpResponseDecodingException.class,
				() -> client.sendRequest(positiveContext(client)));

		HttpErrorResponse response = failure.getResponse();
		assertEquals(body, response.textBody());
		assertEquals(body.getBytes(UTF_8).length, response.bodySize());
		assertFalse(response.bodyTruncated());
		assertTrue(response.decodingFailure().contains("deliberate decoding failure"));
		assertSame(MarshallException.class, failure.getCause().getClass());
	}

	@Test
	public void negativeResponseKeepsEmptyBodyWhenDecoderFailsBeforeReading() {
		GmHttpClient client = client(400, "", ClosingMarshaller.failingAfter(0));

		HttpException failure = assertThrows(HttpException.class, () -> client.sendRequest(negativeContext(client)));

		HttpErrorResponse response = (HttpErrorResponse) failure.getPayload();
		assertEquals("", response.textBody());
		assertEquals(0, response.bodySize());
		assertFalse(response.bodyTruncated());
		assertTrue(response.decodingFailure().contains("deliberate decoding failure"));
	}

	@Test
	public void negativeResponseKeepsBodyWhenDecoderFailsAfterReadingItCompletely() {
		String body = "complete before failure";
		GmHttpClient client = client(400, body, ClosingMarshaller.failingAfter(body.getBytes(UTF_8).length));

		HttpException failure = assertThrows(HttpException.class, () -> client.sendRequest(negativeContext(client)));

		HttpErrorResponse response = (HttpErrorResponse) failure.getPayload();
		assertEquals(body, response.textBody());
		assertEquals(body.getBytes(UTF_8).length, response.bodySize());
		assertFalse(response.bodyTruncated());
	}

	@Test
	public void positiveDecodeFailurePreservesSizeAndTruncationForLargeBody() {
		String body = "x".repeat(48 * 1024);
		GmHttpClient client = client(200, body, ClosingMarshaller.failingAfter(7));

		HttpResponseDecodingException failure = assertThrows(HttpResponseDecodingException.class,
				() -> client.sendRequest(positiveContext(client)));

		HttpErrorResponse response = failure.getResponse();
		assertEquals(16 * 1024, response.textBody().getBytes(UTF_8).length);
		assertEquals(body.getBytes(UTF_8).length, response.bodySize());
		assertTrue(response.bodyTruncated());
		assertEquals(body.substring(0, 16 * 1024), response.textBody());
	}

	@Test
	public void negativeDecodeFailurePreservesSizeAndTruncationForLargeBody() {
		String body = "y".repeat(48 * 1024);
		GmHttpClient client = client(400, body, ClosingMarshaller.failingAfter(7));

		HttpException failure = assertThrows(HttpException.class, () -> client.sendRequest(negativeContext(client)));

		HttpErrorResponse response = (HttpErrorResponse) failure.getPayload();
		assertEquals(16 * 1024, response.textBody().getBytes(UTF_8).length);
		assertEquals(body.getBytes(UTF_8).length, response.bodySize());
		assertTrue(response.bodyTruncated());
		assertEquals(body.substring(0, 16 * 1024), response.textBody());
	}

	@Test
	public void negativeGzipResponseCanBeCapturedAfterDecoderClosesItsView() throws Exception {
		String body = "{\"compressed\":\"Generali-style error body\"}";
		byte[] compressed = gzip(body);
		GmHttpClient client = client(400, () -> gzipInput(compressed), ClosingMarshaller.failingAfter(4));

		HttpException failure = assertThrows(HttpException.class, () -> client.sendRequest(negativeContext(client)));

		HttpErrorResponse response = (HttpErrorResponse) failure.getPayload();
		assertEquals(body, response.textBody());
		assertEquals(body.getBytes(UTF_8).length, response.bodySize());
		assertFalse(response.bodyTruncated());
		assertFalse(response.decodingFailure().contains("closed stream"));
	}

	@Test
	public void captureFailureDoesNotMaskOriginalPositiveDecodingFailure() {
		String body = "transport fails while preserving this response";
		GmHttpClient client = client(200,
				() -> new FailingInputStream(body.getBytes(UTF_8), 12), ClosingMarshaller.failingAfter(2));

		HttpResponseDecodingException failure = assertThrows(HttpResponseDecodingException.class,
				() -> client.sendRequest(positiveContext(client)));

		assertSame(MarshallException.class, failure.getCause().getClass());
		assertEquals(1, failure.getCause().getSuppressed().length);
		assertTrue(failure.getCause().getSuppressed()[0] instanceof IOException);
		assertTrue(failure.getResponse().decodingFailure().contains("deliberate decoding failure"));
		assertTrue(failure.getResponse().decodingFailure().contains("failed to capture the remaining response body"));
		assertEquals(body.substring(0, 12), failure.getResponse().textBody());
		assertEquals(12, failure.getResponse().bodySize());
	}

	@Test
	public void captureFailureDoesNotReplaceMappedNegativeResponse() {
		String body = "negative transport fails while preserving this response";
		GmHttpClient client = client(400,
				() -> new FailingInputStream(body.getBytes(UTF_8), 14), ClosingMarshaller.failingAfter(3));

		HttpException failure = assertThrows(HttpException.class, () -> client.sendRequest(negativeContext(client)));

		HttpErrorResponse response = (HttpErrorResponse) failure.getPayload();
		assertTrue(response.decodingFailure().contains("deliberate decoding failure"));
		assertTrue(response.decodingFailure().contains("failed to capture the remaining response body"));
		assertEquals(body.substring(0, 14), response.textBody());
		assertEquals(14, response.bodySize());
	}

	@Test
	public void successfulPositiveResponseMayCloseDecoderView() {
		NotFound decoded = NotFound.create("decoded response");
		GmHttpClient client = client(200, "valid", ClosingMarshaller.succeedingWith(decoded));

		HttpResponse response = client.sendRequest(positiveContext(client));

		assertSame(decoded, response.payload());
	}

	@Test
	public void successfullyDecodedNegativeResponseStillCapturesTrailingBody() {
		NotFound decoded = NotFound.create("decoded detail");
		String body = "complete negative response";
		GmHttpClient client = client(400, body, ClosingMarshaller.succeedingAfterPrefix(decoded, 3));

		HttpException failure = assertThrows(HttpException.class, () -> client.sendRequest(negativeContext(client)));

		HttpErrorResponse response = (HttpErrorResponse) failure.getPayload();
		assertSame(decoded, response.decodedBody());
		assertEquals(body, response.textBody());
		assertEquals(body.length(), response.bodySize());
		assertFalse(response.bodyTruncated());
	}

	private static HttpRequestContext positiveContext(GmHttpClient client) {
		return HttpRequestContextBuilder.instance(client)
				.requestPath("http://example.test/positive")
				.requestMethod("GET")
				.produces(MIME_TYPE)
				.addSuccessCode(200)
				.defaultSuccessResponseType(NotFound.T)
				.addResponseDecodingLenience(200, false)
				.build();
	}

	private static HttpRequestContext negativeContext(GmHttpClient client) {
		return HttpRequestContextBuilder.instance(client)
				.requestPath("http://example.test/negative")
				.requestMethod("GET")
				.produces(MIME_TYPE)
				.addSuccessCode(200)
				.addResponseMapping(new HttpResponseMapping("400", HttpResponseMapping.Kind.STATUS, InvalidArgument.T, false, false))
				.addResponseMapping(new HttpResponseMapping("4xx", HttpResponseMapping.Kind.BODY_DETAIL, NotFound.T,
						MIME_TYPE, false, false, false, 0))
				.build();
	}

	private static GmHttpClient client(int statusCode, String body, Marshaller marshaller) {
		return client(statusCode, () -> new ByteArrayInputStream(body.getBytes(UTF_8)), marshaller);
	}

	private static GmHttpClient client(int statusCode, Supplier<InputStream> body, Marshaller marshaller) {
		BasicConfigurableMarshallerRegistry registry = new BasicConfigurableMarshallerRegistry();
		registry.setMarshallers(Map.of(MIME_TYPE, marshaller));

		GmHttpClient client = new GmHttpClient();
		client.setMarshallerRegistry(registry);
		client.setStreamPipeFactory(StreamPipes.simpleFactory());
		client.setHttpClientProvider(new StaticHttpClientProvider(statusCode, body));
		return client;
	}

	private static byte[] gzip(String value) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
			gzip.write(value.getBytes(UTF_8));
		}
		return out.toByteArray();
	}

	private static InputStream gzipInput(byte[] compressed) {
		try {
			return new GZIPInputStream(new ByteArrayInputStream(compressed));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final class ClosingMarshaller implements Marshaller {
		private final int bytesToRead;
		private final Object result;
		private final boolean fail;

		private ClosingMarshaller(int bytesToRead, Object result, boolean fail) {
			this.bytesToRead = bytesToRead;
			this.result = result;
			this.fail = fail;
		}

		private static ClosingMarshaller failingAfter(int bytesToRead) {
			return new ClosingMarshaller(bytesToRead, null, true);
		}

		private static ClosingMarshaller succeedingWith(Object result) {
			return new ClosingMarshaller(Integer.MAX_VALUE, result, false);
		}

		private static ClosingMarshaller succeedingAfterPrefix(Object result, int bytesToRead) {
			return new ClosingMarshaller(bytesToRead, result, false);
		}

		@Override
		public Object unmarshall(InputStream in, GmDeserializationOptions options) throws MarshallException {
			try {
				if (bytesToRead == Integer.MAX_VALUE) {
					IOTools.consume(in);
				} else {
					for (int i = 0; i < bytesToRead && in.read() != -1; i++) {
						// Deliberately consume only a prefix.
					}
				}
				in.close();
			} catch (IOException e) {
				throw new MarshallException(e);
			}
			if (fail)
				throw new MarshallException("deliberate decoding failure");
			return result;
		}

		@Override
		public void marshall(OutputStream out, Object value, GmSerializationOptions options) throws MarshallException {
			throw new UnsupportedOperationException();
		}
	}

	private static final class StaticHttpClientProvider implements HttpClientProvider {
		private final CloseableHttpClient client;

		private StaticHttpClientProvider(int statusCode, Supplier<InputStream> body) {
			this.client = new CloseableHttpClient() {
				@Override
				public HttpParams getParams() {
					return null;
				}

				@Override
				public ClientConnectionManager getConnectionManager() {
					return null;
				}

				@Override
				protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context) {
					BasicHttpResponse response = new BasicHttpResponse(
							new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, "test"));
					BasicHttpEntity entity = new BasicHttpEntity();
					entity.setContent(body.get());
					entity.setContentType(MIME_TYPE);
					response.setEntity(entity);
					return (CloseableHttpResponse) Proxy.newProxyInstance(CloseableHttpResponse.class.getClassLoader(),
							new Class<?>[] { CloseableHttpResponse.class }, (proxy, method, arguments) -> {
								if (method.getName().equals("close")) return null;
								return method.invoke(response, arguments);
							});
				}

				@Override
				public void close() {
				}
			};
		}

		@Override
		public CloseableHttpClient provideHttpClient() {
			return client;
		}

		@Override
		public HttpClientBuilder provideClientBuilder() {
			throw new UnsupportedOperationException();
		}

		@Override
		public CloseableHttpClient provideHttpClient(ClientParameters clientParameters) {
			return client;
		}

		@Override
		public HttpClientBuilder provideClientBuilder(ClientParameters clientParameters) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class FailingInputStream extends InputStream {
		private final byte[] bytes;
		private final int failAfter;
		private int position;

		private FailingInputStream(byte[] bytes, int failAfter) {
			this.bytes = bytes;
			this.failAfter = failAfter;
		}

		@Override
		public int read() throws IOException {
			if (position >= failAfter) throw new IOException("deliberate transport read failure");
			if (position >= bytes.length) return -1;
			return bytes[position++] & 0xff;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException {
			if (position >= failAfter) throw new IOException("deliberate transport read failure");
			if (position >= bytes.length) return -1;
			int count = Math.min(length, Math.min(bytes.length, failAfter) - position);
			System.arraycopy(bytes, position, buffer, offset, count);
			position += count;
			return count;
		}
	}
}
