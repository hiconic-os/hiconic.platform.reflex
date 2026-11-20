package hiconic.rx.webapi.client.processing;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Test;

import com.braintribe.model.deployment.http.annotation.*;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.eval.EvalContext;
import com.braintribe.model.generic.eval.Evaluator;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.processing.itw.analysis.JavaTypeAnalysis;
import com.braintribe.model.processing.meta.cmd.CmdResolver;
import com.braintribe.model.processing.meta.cmd.CmdResolverImpl;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.oracle.BasicModelOracle;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.service.api.ServiceRequest;
import com.sun.net.httpserver.HttpServer;

import hiconic.rx.test.common.AbstractRxTest;
import hiconic.rx.webapi.client.api.HttpClient;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpRequestContextBuilder;
import hiconic.rx.webapi.client.api.WebApiClientContract;
import hiconic.rx.webapi.client.model.configuration.WebApiRemoteProcessor;

public class DateParameterMappingTest extends AbstractRxTest {
	private static final Date DATE = Date.from(Instant.parse("2026-11-18T23:30:00Z"));

	@HttpPost @HttpPath("/dates/{pathDate}") @HttpConsumes("application/json")
	@HttpProduces(responseType = DateResponse.class)
	public interface DateRequest extends ServiceRequest {
		EntityType<DateRequest> T = EntityTypes.T(DateRequest.class);
		@HttpPathParam(dateFormat = "yyyy-MM-dd", dateZone = "Europe/Berlin")
		Date getPathDate(); void setPathDate(Date value);
		@HttpQueryParam(dateFormat = "yyyyMMdd", dateZone = "UTC")
		Date getQueryDate(); void setQueryDate(Date value);
		@HttpHeaderParam(value = "X-Date", dateFormat = "dd.MM.yyyy HH:mm", dateZone = "Europe/Berlin")
		Date getHeaderDate(); void setHeaderDate(Date value);
		@HttpQueryParam(dateFormat = "yyyy-MM-dd", dateZone = "Europe/Berlin")
		List<Date> getDates(); void setDates(List<Date> value);
		@HttpBodyParam
		Date getBodyDate(); void setBodyDate(Date value);
		@Override EvalContext<DateResponse> eval(Evaluator<ServiceRequest> evaluator);
	}

	public interface DateResponse extends GenericEntity {
		EntityType<DateResponse> T = EntityTypes.T(DateResponse.class);
		Date getResultDate(); void setResultDate(Date value);
	}

	@Test
	public void sendsDistinctParameterFormatsWithoutChangingJsonBodyOrResponseDefaults() throws Exception {
		verifyRequest(false);
	}

	@Test
	public void localParameterFormatsDoNotOverrideAnExplicitJsonBodyAndResponseFormat() throws Exception {
		verifyRequest(true);
	}

	private void verifyRequest(boolean withBodyFormat) throws Exception {
		AtomicReference<String> uri = new AtomicReference<>(), header = new AtomicReference<>(), body = new AtomicReference<>();
		String responseBody = withBodyFormat ? "{\"resultDate\":\"18/11/2026 23:30\"}"
				: "{\"resultDate\":" + DATE.getTime() + "}";
		HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/", exchange -> {
			uri.set(exchange.getRequestURI().toString()); header.set(exchange.getRequestHeaders().getFirst("X-Date"));
			body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(200, bytes.length);
			exchange.getResponseBody().write(bytes); exchange.close();
		});
		server.start();
		try {
			WebApiRemoteProcessor configuration = WebApiRemoteProcessor.T.create();
			configuration.setBaseUrl("http://localhost:" + server.getAddress().getPort());
			HttpClient client = resolveExportContract(WebApiClientContract.class).clientsFactory().createHttpClient(configuration);
			String defaultJsonBody = null;
			if (!withBodyFormat) {
				// Capture normal JSON encoding without parameter metadata as the baseline.
				client.sendRequest(HttpRequestContextBuilder.instance(client).requestPath("/baseline").requestMethod("POST")
						.consumes("application/json").produces("application/json").payload(DATE)
						.addSuccessCode(200).defaultSuccessResponseType(DateResponse.T).build());
				defaultJsonBody = body.get();
			}
			FixedClientContextResolver resolver = new FixedClientContextResolver(); resolver.setHttpClient(client);
			CmdResolver cmd = metadata(withBodyFormat); resolver.setDomainIdToCmdResolver(id -> cmd);
			ServiceRequestContext service = (ServiceRequestContext) Proxy.newProxyInstance(ServiceRequestContext.class.getClassLoader(),
					new Class<?>[] { ServiceRequestContext.class }, (p, method, args) -> {
						if (method.getName().equals("getDomainId")) return "test";
						if (method.getName().equals("findOrNull")) return null;
						throw new AssertionError(method.getName());
					});
			DateRequest request = DateRequest.T.create(); request.setPathDate(DATE); request.setQueryDate(DATE);
			request.setHeaderDate(DATE); request.setDates(List.of(DATE, new Date(DATE.getTime() + 86_400_000L))); request.setBodyDate(DATE);
			HttpRequestContext context = resolver.resolve(service, request);
			assertEquals("/dates/2026-11-19", context.requestPath());
			Map<String, String> queries = context.queryParameters().filter(p -> !p.getName().equals("dates"))
					.collect(Collectors.toMap(p -> p.getName(), p -> p.getValue()));
			assertEquals("20261118", queries.get("queryDate"));
			assertEquals(List.of("2026-11-19", "2026-11-20"), context.queryParameters().filter(p -> p.getName().equals("dates"))
					.map(p -> p.getValue()).collect(Collectors.toList()));
			DateResponse response = (DateResponse) client.sendRequest(context).payload();
			assertTrue(uri.get().startsWith("/dates/2026-11-19?"));
			assertTrue(uri.get().contains("queryDate=20261118"));
			assertEquals("19.11.2026 00:30", header.get());
			if (withBodyFormat) {
				assertTrue(body.get(), body.get().contains("18/11/2026 23:30"));
			} else {
				assertNull(context.dateFormatting());
				assertEquals(defaultJsonBody, body.get());
			}
			assertEquals(DATE, response.getResultDate());
		} finally { server.stop(0); }
	}

	private CmdResolver metadata(boolean withBodyFormat) {
		GmMetaModel model = GmMetaModel.T.create(); model.setName("test:parameter-date-encoding");
		model.getDependencies().add(ServiceRequest.T.getModel().getMetaModel());
		JavaTypeAnalysis analysis = new JavaTypeAnalysis();
		for (Class<?> type : List.of(DateRequest.class, DateResponse.class)) {
			GmEntityType gmType = (GmEntityType) analysis.getGmType(type); gmType.setDeclaringModel(model); model.getTypes().add(gmType);
		}
		if (withBodyFormat) {
			var formatting = hiconic.rx.webapi.client.model.meta.HttpDateFormatting.T.create();
			formatting.setDateFormat("dd/MM/yyyy HH:mm"); formatting.setDefaultZone("UTC");
			new BasicModelMetaDataEditor(model).onEntityType(DateRequest.T).addMetaData(formatting);
		}
		return CmdResolverImpl.create(new BasicModelOracle(model)).done();
	}
}
