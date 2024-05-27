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
package hiconic.rx.web.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.braintribe.model.service.api.ServiceRequest;

import dev.hiconic.servlet.ddra.endpoints.api.context.HttpRequestSupplier;
import jakarta.servlet.http.HttpServletRequest;

public class HttpRequestSupplierImpl implements HttpRequestSupplier {

	Map<String, HttpServletRequest> registry = new HashMap<>();

	public HttpRequestSupplierImpl() {

	}

	public HttpRequestSupplierImpl(ServiceRequest serviceRequest, HttpServletRequest httpRequest) {
		put(serviceRequest, httpRequest);
	}

	public void put(ServiceRequest serviceRequest, HttpServletRequest httpRequest) {
		Objects.requireNonNull(serviceRequest, "serviceRequest must not be null");

		if (serviceRequest.getGlobalId() == null) {
			serviceRequest.setGlobalId(UUID.randomUUID().toString());
		}

		registry.put( //
				serviceRequest.getGlobalId(), //
				Objects.requireNonNull(httpRequest, "httpRequest must not be null") //
		);
	}

	@Override
	public Optional<HttpServletRequest> getFor(ServiceRequest request) {
		return Optional.ofNullable(registry.get(request.getGlobalId()));
	}

	@Override
	public Collection<HttpServletRequest> getAll() {
		return registry.values();
	}
}
