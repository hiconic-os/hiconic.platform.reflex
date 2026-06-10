package hiconic.rx.security.processing;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.braintribe.model.processing.service.common.context.UserSessionAspect;
import com.braintribe.model.usersession.UserSession;
import com.braintribe.utils.collection.impl.AttributeContexts;

import hiconic.rx.module.api.auth.RoleAuthorization;

public class SecurityRoleAuthorization implements RoleAuthorization {
	private final Set<String> adminRoles;

	public SecurityRoleAuthorization(Set<String> adminRoles) {
		this.adminRoles = adminRoles == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(adminRoles));
	}

	@Override
	public boolean securityActive() {
		return true;
	}

	@Override
	public boolean hasRole(String role) {
		return role != null && effectiveRoles().contains(role);
	}

	@Override
	public boolean hasAnyRole(Collection<String> roles) {
		if (roles == null || roles.isEmpty())
			return false;

		Set<String> effectiveRoles = effectiveRoles();
		for (String role : roles) {
			if (role != null && effectiveRoles.contains(role))
				return true;
		}

		return false;
	}

	@Override
	public boolean hasAllRoles(Collection<String> roles) {
		if (roles == null || roles.isEmpty())
			return false;

		Set<String> effectiveRoles = effectiveRoles();
		for (String role : roles) {
			if (role == null || !effectiveRoles.contains(role))
				return false;
		}

		return true;
	}

	@Override
	public Set<String> adminRoles() {
		return adminRoles;
	}

	private Set<String> effectiveRoles() {
		UserSession userSession = AttributeContexts.peek().findOrNull(UserSessionAspect.class);
		if (userSession == null || userSession.getEffectiveRoles() == null)
			return Collections.emptySet();

		return userSession.getEffectiveRoles();
	}
}
