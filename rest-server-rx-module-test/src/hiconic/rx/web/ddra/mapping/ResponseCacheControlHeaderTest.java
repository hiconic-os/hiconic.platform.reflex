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

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.braintribe.model.cache.CacheType;

import hiconic.rx.web.ddra.endpoints.api.v1.StreamHeaderUtils;
import jakarta.servlet.http.HttpServletResponse;

public class ResponseCacheControlHeaderTest {

	@Test
	public void absentPolicyProducesNoStore() {
		Map<String, String> headers = new HashMap<>();

		StreamHeaderUtils.setResponseCacheControlHeaders(response(headers), null, null, false);

		assertEquals("no-store", headers.get("Cache-Control"));
	}

	@Test
	public void explicitPublicPolicyProducesConfiguredHeader() {
		Map<String, String> headers = new HashMap<>();

		StreamHeaderUtils.setResponseCacheControlHeaders(response(headers), CacheType.publicCache, 300, true);

		assertEquals("public, max-age=300, must-revalidate", headers.get("Cache-Control"));
	}

	private HttpServletResponse response(Map<String, String> headers) {
		return (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
				new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
					if (method.getName().equals("setHeader"))
						headers.put((String) args[0], (String) args[1]);
					return null;
				});
	}
}
