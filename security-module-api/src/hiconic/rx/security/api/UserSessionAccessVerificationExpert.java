package hiconic.rx.security.api;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.usersession.UserSession;

/** Verifies an additional condition whenever an existing session is accessed. */
public interface UserSessionAccessVerificationExpert {
	Reason verifyUserSessionAccess(ServiceRequestContext requestContext, UserSession userSession);
}
