package hiconic.rx.security.api;

import com.braintribe.common.attribute.TypeSafeAttribute;
import com.braintribe.model.securityservice.AuthenticateCredentialsResponse;

/**
 * A trusted, request-local authentication result which has not yet been turned into a user session.
 *
 * <p>This attribute is intended for server-side authentication transports which need to invoke
 * non-authorizable follow-up requests before a user session may be opened.</p>
 */
public interface AuthenticatedCredentialsAttribute extends TypeSafeAttribute<AuthenticateCredentialsResponse> {
}
