package hiconic.platform.reflex.explorer.processing.servlet.auth.aspect;

import java.util.Collection;
import java.util.Optional;

import com.braintribe.model.service.api.ServiceRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthHttpRequestSupplier {

	Optional<HttpServletRequest> getFor(ServiceRequest request);

	Collection<HttpServletRequest> getAll();
}
