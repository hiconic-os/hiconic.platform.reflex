package hiconic.platform.reflex.security.api;

import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.processing.service.api.ServiceRequestContext;
import com.braintribe.model.usersession.UserSession;

public interface UserSessionAccessVerificationExpert {
	Reason verifyUserSessionAccess(ServiceRequestContext requestContext, UserSession userSession);
}
