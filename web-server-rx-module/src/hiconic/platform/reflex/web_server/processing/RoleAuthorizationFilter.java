package hiconic.platform.reflex.web_server.processing;

import java.io.IOException;
import java.util.function.Supplier;

import hiconic.rx.module.api.auth.RoleAuthorization;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class RoleAuthorizationFilter implements Filter {
	private final Supplier<RoleAuthorization> roleAuthorizationSupplier;

	public RoleAuthorizationFilter(Supplier<RoleAuthorization> roleAuthorizationSupplier) {
		this.roleAuthorizationSupplier = roleAuthorizationSupplier;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		RoleAuthorization roleAuthorization = roleAuthorizationSupplier.get();

		if (!roleAuthorization.securityActive() || roleAuthorization.hasAnyRole(roleAuthorization.adminRoles())) {
			chain.doFilter(request, response);
			return;
		}

		if (response instanceof HttpServletResponse) {
			((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		throw new ServletException("Forbidden");
	}
}
