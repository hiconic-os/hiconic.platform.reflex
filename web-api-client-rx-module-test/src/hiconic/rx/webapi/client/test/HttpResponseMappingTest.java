package hiconic.rx.webapi.client.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.braintribe.gm.model.reason.essential.NotFound;

import hiconic.rx.webapi.client.api.HttpRequestContext;
import hiconic.rx.webapi.client.api.HttpRequestContextBuilder;
import hiconic.rx.webapi.client.api.HttpResponseMapping;

public class HttpResponseMappingTest {

	@Test
	public void implicitResponsesStayLenientWhileExplicitMappingsControlLenience() {
		HttpRequestContext implicit = HttpRequestContextBuilder.instance(null)
				.addResponseType(200, NotFound.T)
				.build();
		assertTrue(implicit.lenientResponseDecodingForCode(200));

		HttpRequestContext explicitStrict = HttpRequestContextBuilder.instance(null)
				.addResponseType(200, NotFound.T)
				.addResponseDecodingLenience(200, false)
				.build();
		assertFalse(explicitStrict.lenientResponseDecodingForCode(200));

		HttpRequestContext reasoningLenient = HttpRequestContextBuilder.instance(null)
				.addResponseMapping(new HttpResponseMapping("400", HttpResponseMapping.Kind.BODY, NotFound.T,
						"application/json", true, false, false, 0))
				.build();
		assertTrue(reasoningLenient.lenientResponseDecodingForCode(400));
	}
}
