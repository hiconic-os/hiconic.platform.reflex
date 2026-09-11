package hiconic.rx.security.api;

import java.util.Set;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.user.User;

/** Verifies additional conditions after credentials were authenticated and before a session is created. */
public interface UserSessionOpeningVerificationExpert {
	Reason verifyUserSessionOpening(ServiceRequestContext requestContext, String entryPoint, User user, Set<String> effectiveRoles);
}
