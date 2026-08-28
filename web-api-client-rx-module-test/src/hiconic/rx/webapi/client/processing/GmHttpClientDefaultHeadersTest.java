package hiconic.rx.webapi.client.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.client.methods.RequestBuilder;
import org.junit.Test;

import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpRequestContextBuilder;

public class GmHttpClientDefaultHeadersTest {

	@Test
	public void requestHeadersOverrideConfiguredDefaultsCaseInsensitively() throws Exception {
		GmHttpClient client = new GmHttpClient();
		Map<String, String> defaults = new LinkedHashMap<>();
		defaults.put("Gen-Client-Id", "default-id");
		defaults.put("Gen-Client-Secret", "default-secret");
		client.setDefaultHeaders(defaults);

		// Mutating the configuration source after wiring must not alter a running client.
		defaults.put("Gen-Client-Secret", "changed-secret");

		HttpRequestContext context = HttpRequestContextBuilder.instance(client)
				.requestPath("http://localhost/headers")
				.addHeaderParameter("gen-client-id", "request-id")
				.build();
		RequestBuilder requestBuilder = requestBuilder(client, context);

		assertEquals("request-id", requestBuilder.getFirstHeader("Gen-Client-Id").getValue());
		assertEquals("default-secret", requestBuilder.getFirstHeader("Gen-Client-Secret").getValue());
		assertEquals(1, requestBuilder.getHeaders("Gen-Client-Id").length);
	}

	@Test
	public void rejectsInvalidDefaultHeadersDuringConfiguration() {
		GmHttpClient client = new GmHttpClient();
		assertThrows(IllegalArgumentException.class, () -> client.setDefaultHeaders(Map.of("", "value")));
		assertThrows(IllegalArgumentException.class, () -> {
			Map<String, String> headers = new LinkedHashMap<>();
			headers.put("Header", null);
			client.setDefaultHeaders(headers);
		});
	}

	private RequestBuilder requestBuilder(GmHttpClient client, HttpRequestContext context) throws Exception {
		Method method = GmHttpClient.class.getDeclaredMethod("requestBuilder", HttpRequestContext.class);
		method.setAccessible(true);
		return (RequestBuilder) method.invoke(client, context);
	}
}
