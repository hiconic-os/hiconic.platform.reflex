package hiconic.rx.webapi.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;

import org.junit.Test;

import com.braintribe.gm.model.reason.essential.InvalidArgument;
import com.braintribe.model.generic.annotation.meta.api.MdaHandler;
import com.braintribe.model.generic.annotation.meta.api.MetaDataAnnotations;
import com.braintribe.model.meta.data.MetaData;

public class HttpMetadataAnnotationsTest {
	@com.braintribe.model.deployment.http.annotation.HttpStatusReasoning(globalId = "status-reasoning", status = "4xx,!404",
			reasonType = InvalidArgument.class)
	private static class StatusReasoningAnnotated {}

	@com.braintribe.model.deployment.http.annotation.HttpBodyDetailReasoning(globalId = "body-detail", status = "4xx,5xx",
			reasonType = InvalidArgument.class, mimeType = "application/problem+json", lenient = true)
	private static class BodyDetailReasoningAnnotated {}

	@com.braintribe.model.deployment.http.annotation.HttpSuccessCodes(globalId = "success-codes", value = { 200, 201, 204 })
	private static class SuccessCodesAnnotated {}

	private static class MultipartProperties {
		@com.braintribe.model.deployment.http.annotation.HttpPathParam(globalId = "optional-path", value = "optional", omitSegmentIfNull = true)
		String optionalPath() { return null; }

		@com.braintribe.model.deployment.http.annotation.HttpMultipartTextPart(globalId = "caption-part", value = "caption",
				mimeType = "text/markdown", headers = { "X-Part: caption" })
		String caption() { return null; }

		@com.braintribe.model.deployment.http.annotation.HttpMultipartMarshalledPart(globalId = "details-part", value = "details",
				mimeType = "application/json")
		Object details() { return null; }
	}

	private static class DateParameters {
		@com.braintribe.model.deployment.http.annotation.HttpPathParam(globalId = "date-path", dateFormat = "yyyy-MM-dd", dateZone = "Europe/Berlin")
		java.util.Date path() { return null; }

		@com.braintribe.model.deployment.http.annotation.HttpQueryParam(globalId = "date-query", dateFormat = "yyyyMMdd", dateZone = "UTC")
		java.util.Date query() { return null; }

		@com.braintribe.model.deployment.http.annotation.HttpHeaderParam(globalId = "date-header", dateFormat = "EEE, dd MMM yyyy HH:mm:ss z", dateZone = "GMT")
		java.util.Date header() { return null; }

		@com.braintribe.model.deployment.http.annotation.HttpMultipartTextPart(globalId = "date-multipart", dateFormat = "dd.MM.yyyy", dateZone = "Europe/Berlin")
		java.util.Date multipart() { return null; }

		@com.braintribe.model.deployment.http.annotation.HttpPathParam(globalId = "date-legacy")
		java.util.Date legacy() { return null; }
	}

	@Test
	public void mapsDateFormatAndZoneOnEachTextualMapping() throws Exception {
		assertDateEncoding("path", com.braintribe.model.deployment.http.annotation.HttpPathParam.class, "yyyy-MM-dd", "Europe/Berlin");
		assertDateEncoding("query", com.braintribe.model.deployment.http.annotation.HttpQueryParam.class, "yyyyMMdd", "UTC");
		assertDateEncoding("header", com.braintribe.model.deployment.http.annotation.HttpHeaderParam.class, "EEE, dd MMM yyyy HH:mm:ss z", "GMT");
		assertDateEncoding("multipart", com.braintribe.model.deployment.http.annotation.HttpMultipartTextPart.class, "dd.MM.yyyy", "Europe/Berlin");
		assertDateEncoding("legacy", com.braintribe.model.deployment.http.annotation.HttpPathParam.class, null, null);
	}

	private void assertDateEncoding(String method, Class<? extends Annotation> annotation, String format, String zone) throws Exception {
		hiconic.rx.webapi.client.model.meta.HttpDateEncoding metadata = (hiconic.rx.webapi.client.model.meta.HttpDateEncoding)
				build(DateParameters.class.getDeclaredMethod(method).getAnnotation(annotation));
		assertEquals(format, metadata.getDateFormat());
		assertEquals(zone, metadata.getDateZone());
	}

	@Test
	public void mapsStatusReasoningAnnotationIncludingClassLiteral() {
		hiconic.rx.webapi.client.model.meta.HttpStatusReasoning metadata = build(
				StatusReasoningAnnotated.class.getAnnotation(com.braintribe.model.deployment.http.annotation.HttpStatusReasoning.class));
		assertEquals("4xx,!404", metadata.getStatusCodeExpression());
		assertEquals(InvalidArgument.class.getName(), metadata.getReasonTypeSignature());
	}

	@Test
	public void mapsBodyReasoningMimeType() {
		hiconic.rx.webapi.client.model.meta.HttpBodyDetailReasoning metadata = build(
				BodyDetailReasoningAnnotated.class.getAnnotation(com.braintribe.model.deployment.http.annotation.HttpBodyDetailReasoning.class));
		assertEquals("4xx,5xx", metadata.getStatusCodeExpression());
		assertEquals(InvalidArgument.class.getName(), metadata.getReasonTypeSignature());
		assertEquals("application/problem+json", metadata.getMimeType());
		assertEquals(true, metadata.getLenient());
	}

	@Test
	public void mapsPrimitiveArrayAnnotation() {
		hiconic.rx.webapi.client.model.meta.HttpSuccessCodes metadata = build(
				SuccessCodesAnnotated.class.getAnnotation(com.braintribe.model.deployment.http.annotation.HttpSuccessCodes.class));
		assertEquals(java.util.Arrays.asList(200, 201, 204), metadata.getSuccessCodes());
	}

	@Test
	public void mapsMultipartPropertyAnnotations() throws Exception {
		hiconic.rx.webapi.client.model.meta.params.HttpMultipartTextPart text = build(
				MultipartProperties.class.getDeclaredMethod("caption")
						.getAnnotation(com.braintribe.model.deployment.http.annotation.HttpMultipartTextPart.class));
		assertEquals("caption", text.getParamName());
		assertEquals("text/markdown", text.getMimeType());
		assertEquals(java.util.Collections.singletonList("X-Part: caption"), text.getHeaders());

		hiconic.rx.webapi.client.model.meta.params.HttpMultipartMarshalledPart marshalled = build(
				MultipartProperties.class.getDeclaredMethod("details")
						.getAnnotation(com.braintribe.model.deployment.http.annotation.HttpMultipartMarshalledPart.class));
		assertEquals("details", marshalled.getParamName());
		assertEquals("application/json", marshalled.getMimeType());
	}

	@Test
	public void mapsOptionalPathAnnotation() throws Exception {
		hiconic.rx.webapi.client.model.meta.params.HttpPathParam path = build(
				MultipartProperties.class.getDeclaredMethod("optionalPath")
						.getAnnotation(com.braintribe.model.deployment.http.annotation.HttpPathParam.class));
		assertEquals("optional", path.getParamName());
		org.junit.Assert.assertTrue(path.getOmitSegmentIfNull());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <M extends MetaData> M build(Annotation annotation) {
		MdaHandler handler = MetaDataAnnotations.registry().annoToHandler().get(annotation.annotationType());
		assertNotNull("No gmf.mda handler for " + annotation.annotationType(), handler);
		return (M) handler.buildMdList(annotation, null).get(0);
	}
}
