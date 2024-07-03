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
package hiconic.rx.web.ddra.servlet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import dev.hiconic.servlet.ddra.endpoints.api.context.HttpResponseConfigurer;
import jakarta.servlet.http.HttpServletResponse;

public class HttpResponseConfigurerImpl implements HttpResponseConfigurer {

	Map<Object, Consumer<HttpServletResponse>> registry = new HashMap<>();

	public HttpResponseConfigurerImpl() {

	}

	@Override
	public void applyFor(Object response, Consumer<HttpServletResponse> consumer) {
		registry.put(response, consumer);
	}

	public void consume(Object serviceResponse, HttpServletResponse htttpResponse) {
		// @formatter:off
		registry.getOrDefault(serviceResponse, r -> { /* NOOP */}).accept(htttpResponse);
		// @formatter:on
	}
}
