package hiconic.rx.log.reflection.processing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Set;

import org.junit.Test;

import com.braintribe.gm.model.reason.Maybe;

import hiconic.rx.log.reflection.model.api.GetLogTopology;
import hiconic.rx.log.reflection.model.api.LogReflectionRequest;
import hiconic.rx.module.api.auth.RoleAuthorization;

public class LogReflectionAuthorizationInterceptorTest {
	@Test
	public void permitsRequestsWhenSecurityIsNotInstalled() {
		LogReflectionAuthorizationInterceptor interceptor = interceptor(new StubRoleAuthorization(false, Set.of()), Set.of("reader"));
		GetLogTopology request = GetLogTopology.T.create();

		assertThat(interceptor.processReasoned(null, request).get()).isSameAs(request);
	}

	@Test
	public void enforcesOnlyTheExplicitlyConfiguredRoles() {
		GetLogTopology request = GetLogTopology.T.create();
		LogReflectionAuthorizationInterceptor denied = interceptor(
				new StubRoleAuthorization(true, Set.of("tf-admin")), Set.of("log-reflection-reader"));
		LogReflectionAuthorizationInterceptor allowed = interceptor(
				new StubRoleAuthorization(true, Set.of("log-reflection-reader")), Set.of("log-reflection-reader"));

		Maybe<? extends LogReflectionRequest> denial = denied.processReasoned(null, request);
		assertThat(denial.isUnsatisfied()).isTrue();
		assertThat(denial.whyUnsatisfied().stringify()).contains("reader role is required");
		assertThat(allowed.processReasoned(null, request).get()).isSameAs(request);
	}

	private static LogReflectionAuthorizationInterceptor interceptor(RoleAuthorization authorization, Set<String> roles) {
		LogReflectionAuthorizationInterceptor interceptor = new LogReflectionAuthorizationInterceptor();
		interceptor.setRoleAuthorization(authorization);
		interceptor.setRoles(roles);
		interceptor.setMessage("A log reflection reader role is required");
		return interceptor;
	}

	private record StubRoleAuthorization(boolean securityActive, Set<String> roles) implements RoleAuthorization {
		@Override public boolean hasRole(String role) { return roles.contains(role); }
		@Override public boolean hasAnyRole(Collection<String> required) {
			return required != null && required.stream().anyMatch(roles::contains);
		}
		@Override public boolean hasAllRoles(Collection<String> required) {
			return required != null && !required.isEmpty() && roles.containsAll(required);
		}
		@Override public Set<String> adminRoles() { return Set.of(); }
	}
}
