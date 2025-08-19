package hiconic.platform.reflex.explorer.processing.servlet.auth;

import com.braintribe.model.security.service.config.OpenUserSessionEntryPoint;

import jakarta.servlet.http.HttpServletRequest;

public interface OpenUserSessionConfigurationProvider {

	String findEntryPointName(HttpServletRequest request);

	OpenUserSessionEntryPoint findEntryPoint(HttpServletRequest request);

	String getCookieName(HttpServletRequest request);

}