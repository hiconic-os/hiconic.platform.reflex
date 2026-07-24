package hiconic.rx.webapi.client.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;

import org.junit.Test;

import com.braintribe.model.generic.annotation.meta.api.MdaHandler;
import com.braintribe.model.generic.annotation.meta.api.MetaDataAnnotations;
import com.braintribe.model.meta.data.MetaData;

public class HttpMetadataAnnotationsTest {

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
