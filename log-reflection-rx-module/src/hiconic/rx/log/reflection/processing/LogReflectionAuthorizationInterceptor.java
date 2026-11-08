package hiconic.rx.log.reflection.processing;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.security.reason.Forbidden;
import com.braintribe.model.processing.service.api.ReasonedServicePreProcessor;
import com.braintribe.model.processing.service.api.ServiceRequestContext;

import hiconic.rx.log.reflection.model.api.LogReflectionRequest;
import hiconic.rx.module.api.auth.RoleAuthorization;

/** Authorizes one polymorphic log-reflection request family. */
public class LogReflectionAuthorizationInterceptor implements ReasonedServicePreProcessor<LogReflectionRequest> {
	private RoleAuthorization roleAuthorization;
	private Set<String> roles = Set.of();
	private String message = "Insufficient logging privileges";

	@Override
	public Maybe<? extends LogReflectionRequest> processReasoned(ServiceRequestContext context, LogReflectionRequest request) {
		if (!roleAuthorization.securityActive() || roleAuthorization.hasAnyRole(roles))
			return Maybe.complete(request);

		return Reasons.build(Forbidden.T).text(message).toMaybe();
	}

	@Configurable
	@Required
	public void setRoleAuthorization(RoleAuthorization roleAuthorization) {
		this.roleAuthorization = Objects.requireNonNull(roleAuthorization, "roleAuthorization");
	}

	@Configurable
	public void setRoles(Collection<String> roles) {
		this.roles = roles == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(roles));
	}

	@Configurable
	public void setMessage(String message) {
		this.message = Objects.requireNonNull(message, "message");
	}
}
