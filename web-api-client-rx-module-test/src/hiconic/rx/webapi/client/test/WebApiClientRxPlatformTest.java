package hiconic.rx.webapi.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.braintribe.model.resource.Resource;
import com.sun.net.httpserver.HttpServer;

import hiconic.rx.test.common.AbstractRxTest;
import hiconic.rx.webapi.client.api.HttpClient;
import hiconic.rx.webapi.client.api.HttpMultipartFormData;
import hiconic.rx.webapi.client.api.HttpMultipartPart;
import hiconic.rx.webapi.client.api.HttpMultipartPartKind;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpRequestContextBuilder;
import hiconic.rx.webapi.client.api.WebApiClientContract;
import hiconic.rx.module.api.service.ServiceProcessorRegistration;
import hiconic.rx.webapi.client.model.configuration.HttpUsernamePasswordCredentials;
import hiconic.rx.webapi.client.model.configuration.WebApiRemoteProcessor;

public class WebApiClientRxPlatformTest extends AbstractRxTest {

	@Test
	public void exposesConfiguredClientAsNamedRemoteProcessor() {
		ServiceProcessorRegistration registration = platformContract.serviceProcessing().serviceProcessorRegistry()
				.require("configured-test-client");
		assertTrue(registration.reflectionSupplier().get() instanceof WebApiRemoteProcessor);
		WebApiRemoteProcessor configuration = (WebApiRemoteProcessor) registration.reflectionSupplier().get();
		assertEquals("configured-test-client", configuration.getName());
		assertTrue(configuration.getCredentials() instanceof HttpUsernamePasswordCredentials);
		HttpUsernamePasswordCredentials credentials = (HttpUsernamePasswordCredentials) configuration.getCredentials();
		assertEquals("test-user", credentials.getUser());
		assertEquals("test-password", credentials.getPassword());
		assertTrue(registration.processorSupplier().get() instanceof hiconic.rx.webapi.client.processing.WebApiClientServiceProcessor);
	}

	@Test
	public void sendsMultipartThroughPlatformConfiguredClient() throws Exception {
		AtomicReference<String> bodyRef = new AtomicReference<>();
		AtomicReference<String> contentTypeRef = new AtomicReference<>();
		HttpServer server = server(exchange -> {
			contentTypeRef.set(exchange.getRequestHeaders().getFirst("Content-Type"));
			bodyRef.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
		});
		try {
			HttpClient client = platformClient(server);
			Resource resource = Resource.createTransient(() -> new ByteArrayInputStream("binary-content".getBytes(StandardCharsets.UTF_8)));
			resource.setName("document.txt");
			resource.setMimeType("text/plain");

			HttpMultipartFormData formData = new HttpMultipartFormData("request", "application/json", Arrays.asList(
					new HttpMultipartPart("documents", resource.getName(), resource.getMimeType(), resource),
					new HttpMultipartPart("caption", null, "text/plain; charset=UTF-8", "hello", HttpMultipartPartKind.TEXT,
							Collections.emptyMap()),
					new HttpMultipartPart("details", null, "application/json", Collections.singletonMap("answer", 42),
							HttpMultipartPartKind.MARSHALLED, Collections.singletonMap("Content-ID", "<details>"))));
			HttpRequestContext context = HttpRequestContextBuilder.instance(client)
					.requestPath("/upload")
					.payload(Collections.singletonMap("title", "test"))
					.multipartFormData(formData)
					.build();

			client.sendRequest(context);
			String body = bodyRef.get();
			assertTrue(contentTypeRef.get().startsWith("multipart/form-data; boundary="));
			assertTrue(body.contains("name=\"request\""));
			assertTrue(body.contains("{\"title\":\"test\"}"));
			assertTrue(body.contains("name=\"documents\""));
			assertTrue(body.contains("filename=\"document.txt\""));
			assertTrue(body.contains("binary-content"));
			assertTrue(body.contains("name=\"caption\""));
			assertTrue(body.contains("hello"));
			assertTrue(body.contains("Content-ID: <details>"));
			assertTrue(body.contains("{\"answer\":42}"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void sendsUrlEncodedCollectionsAndRepeatedQueryParameters() throws Exception {
		AtomicReference<String> bodyRef = new AtomicReference<>();
		AtomicReference<String> queryRef = new AtomicReference<>();
		HttpServer server = server(exchange -> {
			queryRef.set(exchange.getRequestURI().getRawQuery());
			bodyRef.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
		});
		try {
			HttpClient client = platformClient(server);
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("single", "a value");
			payload.put("tag", Arrays.asList("one", "two"));
			HttpRequestContext context = HttpRequestContextBuilder.instance(client)
					.requestPath("/form")
					.consumes("application/x-www-form-urlencoded")
					.addQueryParameter("filter", "one")
					.addQueryParameter("filter", "two")
					.payload(payload)
					.build();

			client.sendRequest(context);
			assertEquals("filter=one&filter=two", queryRef.get());
			assertEquals("single=a+value&tag=one&tag=two", bodyRef.get());
		} finally {
			server.stop(0);
		}
	}

	private HttpClient platformClient(HttpServer server) {
		WebApiRemoteProcessor configuration = WebApiRemoteProcessor.T.create();
		configuration.setName("test-client");
		configuration.setBaseUrl("http://localhost:" + server.getAddress().getPort());
		return resolveExportContract(WebApiClientContract.class).clientsFactory().createHttpClient(configuration);
	}

	private HttpServer server(ExchangeConsumer consumer) throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/", exchange -> {
			try {
				try {
					consumer.accept(exchange);
				} catch (Exception e) {
					throw new IOException(e);
				}
				exchange.sendResponseHeaders(204, -1);
			} finally {
				exchange.close();
			}
		});
		server.start();
		return server;
	}

	@FunctionalInterface
	private interface ExchangeConsumer {
		void accept(com.sun.net.httpserver.HttpExchange exchange) throws Exception;
	}
}
