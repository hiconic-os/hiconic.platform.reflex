package hiconic.rx.module.api.auth;

import java.util.Collection;
import java.util.Set;

public interface RoleAuthorization {
	boolean securityActive();

	boolean hasRole(String role);
	boolean hasAnyRole(Collection<String> roles);
	boolean hasAllRoles(Collection<String> roles);

	Set<String> adminRoles();
}
