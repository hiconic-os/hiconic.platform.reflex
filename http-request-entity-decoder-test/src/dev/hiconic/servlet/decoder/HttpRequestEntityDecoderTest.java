// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package dev.hiconic.servlet.decoder;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.exception.HttpException;
import com.braintribe.testing.test.AbstractTest;
import com.braintribe.utils.DateTools;
import com.braintribe.utils.lcd.CollectionTools;

import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoder;
import dev.hiconic.servlet.decoder.api.HttpRequestEntityDecoderOptions;
import dev.hiconic.servlet.decoder.api.StandardHeadersMapper;
import dev.hiconic.servlet.decoder.model.CustomPropertiesEntity;
import dev.hiconic.servlet.decoder.model.InvalidPropertiesEntity;
import dev.hiconic.servlet.decoder.model.InvalidPropertiesParent;
import dev.hiconic.servlet.decoder.model.MixedPropertiesEntity;
import dev.hiconic.servlet.decoder.model.MixedPropertiesParent;
import dev.hiconic.servlet.decoder.model.StandardHttpHeadersEntity;
import dev.hiconic.servlet.decoder.model.TestEnumeration;
import jakarta.servlet.http.HttpServletRequest;

public class HttpRequestEntityDecoderTest extends AbstractTest {

	@Test
	public void decodeStandardHeadersWithPropertiesMapper() {
		//@formatter:off
		HttpServletRequest request = request()
				.header("aCcEpT", "a1, a2", "a3")
				.header("accEpT-encoding", "ae1", "ae2, ae3")
				.header("AuThOriZation", "auth")
				.header("CONTENT-lEngTH", "12345")
				.header("Date", "Wed, 15 Nov 1995 06:25:24 GMT")
				.build();
		//@formatter:on

		StandardHttpHeadersEntity target1 = StandardHttpHeadersEntity.T.createPlain();
		MixedPropertiesEntity target2 = MixedPropertiesEntity.T.createPlain();
		
		//@formatter:off
		HttpRequestEntityDecoder.createFor(request)
				.target("t1", target1, StandardHeadersMapper.mapToProperties(StandardHttpHeadersEntity.T))
				.target("t2", target2, StandardHeadersMapper.mapToProperties(MixedPropertiesParent.T))
				.decode();
		//@formatter:on

		Assert.assertEquals("Accept may accept list of values", CollectionTools.getList("a1", "a2", "a3"), target1.getAccept());
		Assert.assertEquals("Accept-Encoding may accept list of values", CollectionTools.getSet("ae1", "ae2", "ae3"), target1.getAcceptEncoding());
		Assert.assertEquals("Content-Length should be decoded as an int", 12345, target1.getContentLength());
		Assert.assertEquals("Date in the header should be parsed using HttpRequest's standard methods", DateTools.parseDate("1995-11-15T06:25:24.000+0000"), target1.getDate());
		Assert.assertEquals("Authorization should be parsed properly in standard headers", "auth", target1.getAuthorization());

		Assert.assertEquals("Standard headers should be set on ALL header entities", CollectionTools.getList("a1", "a2", "a3"), target2.getAccept());
		Assert.assertNull("AcceptEncoding is not in MixedPropertiesParent", target2.getAcceptEncoding());
		Assert.assertNull("Generic headers should not be set", target2.getStringProperty());
		Assert.assertEquals("Generic headers should not be set", 0, target2.getIntProperty());
		Assert.assertEquals("Generic headers should not be set", false, target2.getBooleanProperty());
	}

	@Test
	public void decodeStandardHeadersWithDeclaredPropertyMapper() {
		//@formatter:off
		HttpServletRequest request = request()
				.header("aCcEpT", "a1, a2", "a3")
				.header("accEpT-encoding", "ae1", "ae2, ae3")
				.header("CONTENT-lEngTH", "12345")
				.header("Date", "Wed, 15 Nov 1995 06:25:24 GMT")
				.build();
		//@formatter:on

		StandardHttpHeadersEntity target = StandardHttpHeadersEntity.T.createPlain();
		
		//@formatter:off
		HttpRequestEntityDecoder.createFor(request)
				.target("t", target, StandardHeadersMapper.mapToDeclaredProperties(StandardHttpHeadersEntity.T))
				.decode();
		//@formatter:on

		Assert.assertNull("Accept is a property of StandardHttpHeadersParent and should not be parsed", target.getAccept());
		Assert.assertEquals("Content-Length is a property of StandardHttpHeadersParent and should not be parsed", 0, target.getContentLength());
		Assert.assertEquals("Accept-Encoding may accept list of values", CollectionTools.getSet("ae1", "ae2", "ae3"), target.getAcceptEncoding());
		Date expectedDate = DateTools.parseDate("1995-11-15T06:25:24.000+0000");
		Date actualDate = target.getDate();
		Assert.assertEquals("Date in the header should be parsed using HttpRequest's standard methods (expected: "+DateTools.encode(expectedDate, DateTools.ISO8601_DATE_WITH_MS_FORMAT)+", actual: "+DateTools.encode(actualDate, DateTools.ISO8601_DATE_WITH_MS_FORMAT)+")", expectedDate, actualDate);
		Assert.assertNull("Authorization is not part of the request", target.getAuthorization());
	}
	
	@Test
	public void decodeGenericHeaders() {
		//@formatter:off
		HttpServletRequest request = request()
				.header("gm-accept", "a1, a2", "a3")
				.header("gm-acceptEncoding", "ae1", "ae2, ae3")
				.header("gm-stringProperty", "s1, s2")
				.header("gm-intProperty", "67890")
				.header("gm-booleanProperty", "true")
				.header("gm-longProperty", "123456789101112")
				.header("gm-dateProperty", "2017-06-11T11:35:00.000+0000")
				.header("gm-enumProperty", "VALUE_1")
				.header("gm-globalId", "xxx")
				.build();
		//@formatter:on

		MixedPropertiesEntity target2 = MixedPropertiesEntity.T.createPlain();
		CustomPropertiesEntity target3 = CustomPropertiesEntity.T.createPlain();
		
		//@formatter:off
		HttpRequestEntityDecoder.createFor(request)
				.target("t2", target2)
				.target("t3", target3)
				.decode();
		//@formatter:on

		Assert.assertEquals("Generic header properties should not be split by \",\"", "s1, s2", target2.getStringProperty());
		Assert.assertEquals("int properties should be parsed properly in headers", 67890, target2.getIntProperty());
		Assert.assertEquals("boolean properties should be parsed properly in headers", true, target2.getBooleanProperty());
		Assert.assertEquals("List<String> properties should be parsed properly in headers", CollectionTools.getList("a1, a2", "a3"), target2.getAccept());
		Assert.assertEquals("Set<String> properties should be parsed properly in headers", CollectionTools.getSet("ae1", "ae2, ae3"), target2.getAcceptEncoding());
		Assert.assertEquals("globalId property should be properly parsed in headers", "xxx", target2.getGlobalId());

		Assert.assertEquals("Dates in headers should be parsed properly", DateTools.parseDate("2017-06-11T11:35:00.000+0000"), target3.getDateProperty());
		Assert.assertEquals("Enumerations in headers should be parsed properly", TestEnumeration.VALUE_1, target3.getEnumProperty());
		Assert.assertNull("stringProperty should be set on target2 as it is before in the targets' list", target3.getStringProperty());
		Assert.assertFalse("booleanProperty should be set on target2 as it is before in the targets' list", target3. getBooleanProperty());
		Assert.assertEquals("intProperty should be set on target2 as it is before in the targets' list", 0, target3.getIntProperty());
	}

	@Test
	public void decodeGenericHeadersWithPrefix() {
		//@formatter:off
		HttpServletRequest request = request()
				.header("gm-t3.stringProperty", "s1, s2")
				.build();
		//@formatter:on

		MixedPropertiesEntity target2 = MixedPropertiesEntity.T.createPlain();
		CustomPropertiesEntity target3 = CustomPropertiesEntity.T.createPlain();

		//@formatter:off
		HttpRequestEntityDecoder.createFor(request)
				.target("t2", target2)
				.target("t3", target3)
				.decode();
		//@formatter:on

		Assert.assertNull("The header has a prefix corresponding to target3, target2's property should not be set", target2.getStringProperty());
		Assert.assertEquals("The header has a prefix corresponding to target3, property should be set", "s1, s2", target3.getStringProperty());
	}

	@Test
	public void decodeUrlParameters() {
		//@formatter:off
		HttpServletRequest request = request()
				.parameter("accept", "a1, a2", "a3")
				.parameter("acceptEncoding", "ae1, ae2", "ae3")
				.parameter("authorization", "auth")
				.parameter("date", "2017-06-11T11:35:00.000+0000")
				.parameter("age", "1234")
				.parameter("booleanProperty", "true")
				.parameter("floatProperty", "123.456")
				.parameter("longProperty", "123456789101112")
				.parameter("dateProperty", "2017-06-11T11:35:00.000+0000")
				.parameter("enumProperty", "VALUE_1")
				.parameter("globalId","xxx")
				.build();
		//@formatter:on
		
		decodeUrlParameters(request);
	}
	
	@Test
	public void decodeUrlParametersWithHyphens() {
		//@formatter:off
		HttpServletRequest request = request()
				.parameter("accept", "a1, a2", "a3")
				.parameter("accept-encoding", "ae1, ae2", "ae3")
				.parameter("authorization", "auth")
				.parameter("date", "2017-06-11T11:35:00.000+0000")
				.parameter("age", "1234")
				.parameter("boolean-property", "true")
				.parameter("float-property", "123.456")
				.parameter("long-property", "123456789101112")
				.parameter("date-property", "2017-06-11T11:35:00.000+0000")
				.parameter("enum-property", "VALUE_1")
				.parameter("global-id","xxx")
				.build();
		//@formatter:on
		
		decodeUrlParameters(request);
	}
	
	public void decodeUrlParameters(HttpServletRequest request) {

		StandardHttpHeadersEntity target1 = StandardHttpHeadersEntity.T.create();
		MixedPropertiesEntity target2 = MixedPropertiesEntity.T.create();
		CustomPropertiesEntity target3 = CustomPropertiesEntity.T.create();

		//@formatter:off
		HttpRequestEntityDecoder.createFor(request)
				.target("t1", target1)
				.target("t2", target2)
				.target("t3", target3)
				.decode();
		//@formatter:on
		
		Assert.assertEquals("List properties in URL should not be split by comma", CollectionTools.getList("a1, a2", "a3"), target1.getAccept());
		Assert.assertEquals("List properties in URL can also be assigned to sets", CollectionTools.getSet("ae1, ae2", "ae3"), target1.getAcceptEncoding());
		Assert.assertEquals("String properties should be properly parsed in URL parameters", "auth", target1.getAuthorization());
		Assert.assertEquals("Date properties should be properly parsed in URL parameters", DateTools.parseDate("2017-06-11T11:35:00.000+0000"), target1.getDate());
		Assert.assertEquals("globalId property should be properly parsed in URL parameters", "xxx", target1.getGlobalId());
		
		Assert.assertEquals("int properties should be parsed properly in URL parameters", 1234, target2.getAge());
		Assert.assertEquals("boolean properties should be parsed properly in URL parameters", true, target2.getBooleanProperty());
		Assert.assertEquals("float properties should be parsed properly in URL parameters", 123.456, target2.getFloatProperty(), 0.00001);
		Assert.assertEquals("long properties should be parsed properly in URL parameters", 123456789101112l, target2.getLongProperty());
		Assert.assertTrue("accept property should be set only in target1 because it comes before in the list", target2.getAccept().isEmpty());
		Assert.assertNull("authorization property should be set only in target1 because it comes before in the list", target2.getAuthorization());

		Assert.assertEquals("Dates should be parsed properly in URL parameters", DateTools.parseDate("2017-06-11T11:35:00.000+0000"), target3.getDateProperty());
		Assert.assertEquals("Enumerations should be parsed properly in URL parameters", TestEnumeration.VALUE_1, target3.getEnumProperty());
		Assert.assertNull("stringProperty should be set on target2 as it is before in the targets' list", target3.getStringProperty());
		Assert.assertFalse("booleanProperty should be set on target2 as it is before in the targets' list", target3. getBooleanProperty());
		Assert.assertEquals("intProperty should be set on target2 as it is before in the targets' list", 0, target3.getIntProperty());
	}
	
	public void decodeHeaderParametersOverrideUrlParameters() {
		//@formatter:off
		HttpServletRequest request = request()
				.parameter("accept", "a1, a2")
				.header("Accept", "a3, a4")
				.parameter("acceptEncoding", "ae1, ae2")
				.header("gm-acceptEncoding", "ae3, ae4")
				.parameter("stringProperty", "s1, s2")
				.header("gm-stringProperty", "s2, s3")
				.parameter("intProperty", "12345")
				.header("gm-intProperty", "67890")
				.build();
		//@formatter:on

		MixedPropertiesEntity target = MixedPropertiesEntity.T.createPlain();
		
		//@formatter:off
		HttpRequestEntityDecoder.createFor(request)
				.target("t", target)
				.decode();
		//@formatter:on

		Assert.assertEquals("Lists should be concatenated when populated in URL parameter and header", CollectionTools.getList("a1, a2", "a3", "a4"), target.getAccept());
		Assert.assertEquals("Sets should be concatenated when populated in URL parameter and header", CollectionTools.getSet("ae1, ae2", "ae3", "ae4"), target.getAcceptEncoding());
		Assert.assertEquals("Strings should be replaced when populated in URL parameter and header", "s2, s3", target.getStringProperty());
		Assert.assertEquals("Ints should be replaced when populated in URL parameter and header", 67890, target.getIntProperty());
	}

	@Test
	public void decodeUrlParametersWithPrefix() {
		//@formatter:off
		HttpServletRequest request = request()
				.parameter("t2.authorization", "auth")
				.build();
		//@formatter:on

		StandardHttpHeadersEntity target1 = StandardHttpHeadersEntity.T.createPlain();
		MixedPropertiesEntity target2 = MixedPropertiesEntity.T.createPlain();

		//@formatter:off
		HttpRequestEntityDecoder.createFor(request)
				.target("t1", target1)
				.target("t2", target2)
				.decode();
		//@formatter:on

		Assert.assertNull("The authorization parameter has a prefix and should be set in target2", target1.getAuthorization());
		Assert.assertEquals("The authorization parameter has a prefix and should be set in target2", "auth", target2.getAuthorization());
	}

	@Test
	public void decodeIgnoreUnmappedUrlParameters() {
		//@formatter:off
		HttpServletRequest request = request()
				.parameter("notExisting", "value")
				.build();
		//@formatter:on
		
		StandardHttpHeadersEntity target = StandardHttpHeadersEntity.T.createPlain();
		HttpRequestEntityDecoder.createFor(request, HttpRequestEntityDecoderOptions.defaults().setIgnoringUnmappedUrlParameters(true)).target("t", target).decode();
	}

	@Test
	public void decodeIgnoreUnmappedHeaderParameters() {
		//@formatter:off
		HttpServletRequest request = request()
				.header("gm-notExisting", "value")
				.build();
		//@formatter:on
		
		StandardHttpHeadersEntity target = StandardHttpHeadersEntity.T.createPlain();
		HttpRequestEntityDecoder.createFor(request, HttpRequestEntityDecoderOptions.defaults().setIgnoringUnmappedHeaders(true)).target("t", target).decode();
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidDateInUrlParameters() {
		decodeInvalidParameter("invalidDate", "2017-06-11T11:35:00.000+0000", false);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidDateInStandardHeaders() {
		decodeInvalidParameter("Date", "2017-06-11T11:35:00.000+0000", true);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidMapPropertyInUrlParameters() {
		decodeInvalidParameter("mapProperty", "value", false);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidMapPropertyInHeaders() {
		decodeInvalidParameter("gm-mapProperty", "value", true);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidEntityPropertyInUrlParameters() {
		decodeInvalidParameter("entityProperty", "value", false);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidEntityPropertyInHeaders() {
		decodeInvalidParameter("gm-entityProperty", "value", true);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidObjectPropertyInHeaders() {
		decodeInvalidParameter("gm-objectProperty", "value", true);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidIntPropertyInUrlParameters() {
		decodeInvalidParameter("intProperty", "NotAnInt", false);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidIntPropertyInHeaders() {
		decodeInvalidParameter("gm-intProperty", "NotAnInt", true);
	}

	@Test(expected=HttpException.class)
	public void decodeInvalidIntPropertyInStandardHeaders() {
		decodeInvalidParameter("Content-Length", "NotAnInt", true);
	}

	@Test(expected=HttpException.class)
	public void decodePropertyNotFoundInUrlParameters() {
		decodeInvalidParameter("doesNotExist", "value", false);
		System.out.println("Succeeded-");
	}

	@Test(expected=HttpException.class)
	public void decodePropertyNotFoundInHeaders() {
		decodeInvalidParameter("gm-doesNotExist", "value", true);
	}

	@Test
	public void decodePropertyNotFoundInStandardHeaders() {
		decodeInvalidParameter("Some-Unknown-Standard-Header", "value", true);
	}

	private void decodeInvalidParameter(String parameterName, String parameterValue, boolean isHeader) {
		MockHttpServletRequestBuilder builder = request();
		if(isHeader) {
			builder.header(parameterName, parameterValue);	
		} else {
			builder.parameter(parameterName, parameterValue);
		}
		HttpServletRequest request =	builder.build();

		//@formatter:off
		HttpRequestEntityDecoder.createFor(request)
				.target("t", InvalidPropertiesEntity.T.create(), StandardHeadersMapper.mapToProperties(InvalidPropertiesParent.T))
				.decode();
		//@formatter:on
	}
	
	private MockHttpServletRequestBuilder request() {
		return new MockHttpServletRequestBuilder();
	}
}
