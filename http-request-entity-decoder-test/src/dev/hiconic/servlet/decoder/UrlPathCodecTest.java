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

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.model.generic.GenericEntity;

import dev.hiconic.servlet.decoder.api.UrlPathCodec;
import dev.hiconic.servlet.decoder.model.TestEntityWithStringProperties;

public class UrlPathCodecTest {

	//@formatter:off
	private static final UrlPathCodec<GenericEntity> CODEC = UrlPathCodec.create()
			.mappedSegment("property1").constantSegment("c1").mappedSegment("property2");
	//@formatter:on

	//@formatter:off
	private static final UrlPathCodec<GenericEntity> CODEC_OPTIONAL = UrlPathCodec.create()
			.mappedSegment("property1", true).constantSegment("c1").mappedSegment("property2");
	//@formatter:on

	//@formatter:off
	private static final UrlPathCodec<GenericEntity> CODEC_LONG = UrlPathCodec.create()
			.mappedSegment("property1").constantSegment("c1").constantSegment("c2").constantSegment("c3")
			.mappedSegment("property2").constantSegment("c4").constantSegment("c5");
	//@formatter:on

	@Test
	public void decode() {
		TestEntityWithStringProperties entity = TestEntityWithStringProperties.T.create();

		CODEC.decode(() -> entity, "aaa/bbb/ccc/ddd");

		Assert.assertEquals("aaa", entity.getProperty1());
		Assert.assertEquals("ccc", entity.getProperty2());
		Assert.assertNull(entity.getProperty3());
	}

	@Test
	public void decodeOptional() {
		TestEntityWithStringProperties entity = TestEntityWithStringProperties.T.create();

		CODEC_OPTIONAL.decode(() -> entity, "aaa/bbb/ccc");

		Assert.assertEquals("aaa", entity.getProperty1());
		Assert.assertEquals("ccc", entity.getProperty2());
		Assert.assertNull(entity.getProperty3());
	}

	@Test
	public void decodeOptionalNotPresent() {
		TestEntityWithStringProperties entity = TestEntityWithStringProperties.T.create();

		CODEC_OPTIONAL.decode(() -> entity, "aaa/bbb");

		Assert.assertNull(entity.getProperty1());
		Assert.assertEquals("bbb", entity.getProperty2());
		Assert.assertNull(entity.getProperty3());
	}
	
	@Test
	public void decodeLongUrl() {
		TestEntityWithStringProperties entity = TestEntityWithStringProperties.T.create();

		CODEC.decode(() -> entity, "aaa/bbb/ccc/ddd/eee/fff/ggg");

		Assert.assertEquals("aaa", entity.getProperty1());
		Assert.assertEquals("ccc", entity.getProperty2());
		Assert.assertNull(entity.getProperty3());
	}

	@Test
	public void decodeTooManySegments() {
		TestEntityWithStringProperties entity = TestEntityWithStringProperties.T.create();

		CODEC_LONG.decode(() -> entity, "aaa/bbb/ccc");

		Assert.assertEquals("aaa", entity.getProperty1());
		Assert.assertNull(entity.getProperty2());
		Assert.assertNull(entity.getProperty3());
	}

	@Test
	public void encode() {
		TestEntityWithStringProperties entity = TestEntityWithStringProperties.T.create();
		entity.setProperty1("property1");
		entity.setProperty2("property2");

		String value = CODEC.encode(entity);

		Assert.assertEquals("property1/c1/property2", value);
	}

	@Test
	public void encodeNullProperty() {
		TestEntityWithStringProperties entity = TestEntityWithStringProperties.T.create();
		entity.setProperty2("property2");

		String value = CODEC.encode(entity);

		Assert.assertEquals("c1/property2", value);
	}
}
