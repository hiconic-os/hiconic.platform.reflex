// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
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
