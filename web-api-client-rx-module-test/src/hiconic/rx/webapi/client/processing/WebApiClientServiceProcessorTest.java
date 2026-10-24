package hiconic.rx.webapi.client.processing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.model.reason.essential.ParseError;

import hiconic.rx.webapi.client.api.HttpErrorResponse;
import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpRequestContextBuilder;
import hiconic.rx.webapi.client.api.HttpResponseMapping;
import hiconic.rx.webapi.model.reason.HttpStatusReason;
import hiconic.rx.webapi.model.reason.HttpStatusWithEntityBody;
import hiconic.rx.webapi.model.reason.HttpStatusWithTextBody;

public class WebApiClientServiceProcessorTest {

	@Test
	public void genericDetailIsCarriedByStatusReason() {
		HttpRequestContext context = HttpRequestContextBuilder.instance(null)
				.addResponseMapping(new HttpResponseMapping("400", HttpResponseMapping.Kind.STATUS, InvalidArgument.T, false, false))
				.addResponseMapping(new HttpResponseMapping("4xx", HttpResponseMapping.Kind.BODY_DETAIL, NotFound.T, false, false))
				.build();
		NotFound detail = NotFound.T.create();
		detail.setText("remote problem details");
		HttpErrorResponse response = new HttpErrorResponse(detail, "{\"text\":\"remote problem details\"}",
				"application/problem+json", NotFound.class.getName(), 40, false, null);

		Reason result = WebApiClientServiceProcessor.mapFailure(context, 400, response);

		assertTrue(result instanceof InvalidArgument);
		assertEquals("remote problem details", result.getText());
		assertTrue(result.getReasons().get(0) instanceof HttpStatusWithEntityBody);
		HttpStatusWithEntityBody status = (HttpStatusWithEntityBody) result.getReasons().get(0);
		assertSame(detail, status.getBody());
		assertStatus(status, 400);
	}

	@Test
	public void undecodableDetailFallsBackToTextAndRetainsParseError() {
		HttpRequestContext context = HttpRequestContextBuilder.instance(null)
				.addResponseMapping(new HttpResponseMapping("400", HttpResponseMapping.Kind.STATUS, InvalidArgument.T, false, false))
				.addResponseMapping(new HttpResponseMapping("4xx", HttpResponseMapping.Kind.BODY_DETAIL, NotFound.T, false, false))
				.build();
		String body = "not-json";
		HttpErrorResponse response = new HttpErrorResponse(null, body, "application/problem+json", NotFound.class.getName(),
				body.length(), false, "Could not decode response");

		Reason result = WebApiClientServiceProcessor.mapFailure(context, 400, response);

		assertTrue(result instanceof InvalidArgument);
		assertSame(HttpStatusWithTextBody.T, result.getReasons().get(0).entityType());
		HttpStatusWithTextBody status = (HttpStatusWithTextBody) result.getReasons().get(0);
		assertEquals(body, status.getBody());
		assertEquals(NotFound.class.getName(), status.getExpectedBodyTypeSignature());
		assertEquals("application/problem+json", status.getMimeType());
		assertTrue(status.getReasons().get(0) instanceof ParseError);
		assertStatus(status, 400);
	}

	@Test
	public void meaningfulRemoteReasonRemainsTheMainReason() {
		HttpRequestContext context = HttpRequestContextBuilder.instance(null)
				.addResponseMapping(new HttpResponseMapping("409", HttpResponseMapping.Kind.BODY, NotFound.T, false, false))
				.build();
		NotFound remoteReason = NotFound.T.create();
		remoteReason.setText("meaningful remote reason");
		HttpErrorResponse response = new HttpErrorResponse(remoteReason, "{}", "application/json", NotFound.class.getName(), 2, false, null);

		Reason result = WebApiClientServiceProcessor.mapFailure(context, 409, response);

		assertSame(remoteReason, result);
		assertStatus(result.getReasons().get(0), 409);
	}

	private static void assertStatus(Reason reason, int expectedStatus) {
		assertTrue(reason instanceof HttpStatusReason);
		assertEquals(Integer.valueOf(expectedStatus), ((HttpStatusReason) reason).getStatusCode());
	}
}
