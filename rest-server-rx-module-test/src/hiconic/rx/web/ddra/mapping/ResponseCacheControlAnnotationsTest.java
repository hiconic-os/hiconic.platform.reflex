// ============================================================================
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
package hiconic.rx.web.ddra.mapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;

import org.junit.Test;

import com.braintribe.model.cache.CacheType;
import com.braintribe.model.generic.annotation.meta.api.MdaHandler;
import com.braintribe.model.generic.annotation.meta.api.MetaDataAnnotations;
import com.braintribe.model.generic.annotation.meta.api.analysis.MdaAnalysisContext;
import com.braintribe.model.meta.data.MetaData;
import com.braintribe.model.weaving.data.ProtoHasMetaData;

import hiconic.rx.webapi.model.meta.CacheTypeOverride;
import hiconic.rx.webapi.model.meta.HttpRequestMethod;

public class ResponseCacheControlAnnotationsTest {

	@hiconic.rx.webapi.model.annotation.ResponseCacheControl(value = CacheType.publicCache, maxAge = 300, mustRevalidate = true)
	private static class CacheableRequest {
	}

	@hiconic.rx.webapi.model.annotation.RequestMapping(path = "/private", method = HttpRequestMethod.GET,
			responseCacheType = CacheTypeOverride.PRIVATE_CACHE, responseCacheMaxAge = 60, responseCacheMustRevalidate = true)
	private static class PrivateMappingRequest {
	}

	@Test
	public void mapsStandaloneResponseCacheControl() {
		hiconic.rx.webapi.model.meta.ResponseCacheControl metadata = build(
				CacheableRequest.class.getAnnotation(hiconic.rx.webapi.model.annotation.ResponseCacheControl.class));

		assertEquals(CacheType.publicCache, metadata.getType());
		assertEquals(Integer.valueOf(300), metadata.getMaxAge());
		assertTrue(metadata.getMustRevalidate());
	}

	@Test
	public void mapsCompleteRequestMappingCacheOverride() {
		hiconic.rx.webapi.model.meta.RequestMapping metadata = build(
				PrivateMappingRequest.class.getAnnotation(hiconic.rx.webapi.model.annotation.RequestMapping.class));

		assertEquals(CacheTypeOverride.PRIVATE_CACHE, metadata.getResponseCacheType());
		assertEquals(Integer.valueOf(60), metadata.getResponseCacheMaxAge());
		assertTrue(metadata.getResponseCacheMustRevalidate());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <M extends MetaData> M build(Annotation annotation) {
		MdaHandler handler = MetaDataAnnotations.registry().annoToHandler().get(annotation.annotationType());
		assertNotNull("No gmf.mda handler for " + annotation.annotationType(), handler);
		ProtoHasMetaData target = proxy(ProtoHasMetaData.class, "getGlobalId", "test-target");
		MdaAnalysisContext context = proxy(MdaAnalysisContext.class, "getTarget", target);
		return (M) handler.buildMdList(annotation, context).get(0);
	}

	@SuppressWarnings("unchecked")
	private <T> T proxy(Class<T> type, String methodName, Object result) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
				(proxy, method, args) -> method.getName().equals(methodName) ? result : null);
	}
}
