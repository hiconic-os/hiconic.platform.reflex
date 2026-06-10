package hiconic.rx.platform.processing.auth;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import hiconic.rx.module.api.auth.RoleAuthorization;

public class DefaultRoleAuthorization implements RoleAuthorization {
	@Override
	public boolean securityActive() {
		return false;
	}

	@Override
	public boolean hasRole(String role) {
		return false;
	}

	@Override
	public boolean hasAnyRole(Collection<String> roles) {
		return false;
	}

	@Override
	public boolean hasAllRoles(Collection<String> roles) {
		return false;
	}

	@Override
	public Set<String> adminRoles() {
		return Collections.emptySet();
	}
}
