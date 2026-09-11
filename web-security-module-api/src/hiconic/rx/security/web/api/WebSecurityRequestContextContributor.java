package hiconic.rx.security.web.api;

import com.braintribe.common.attribute.AttributeContextBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Contributes HTTP-derived attributes before web authentication is evaluated. */
public interface WebSecurityRequestContextContributor {
	void contribute(HttpServletRequest request, HttpServletResponse response, AttributeContextBuilder contextBuilder);
}
