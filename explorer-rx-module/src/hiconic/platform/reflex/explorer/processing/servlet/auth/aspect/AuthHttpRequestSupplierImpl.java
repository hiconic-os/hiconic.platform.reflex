package hiconic.platform.reflex.explorer.processing.servlet.auth.aspect;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.braintribe.model.service.api.ServiceRequest;

import jakarta.servlet.http.HttpServletRequest;

public class AuthHttpRequestSupplierImpl implements AuthHttpRequestSupplier {
	Map<String, HttpServletRequest> registry = new HashMap<>();

	public AuthHttpRequestSupplierImpl() {

	}

	public AuthHttpRequestSupplierImpl(ServiceRequest serviceRequest, HttpServletRequest httpRequest) {
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
